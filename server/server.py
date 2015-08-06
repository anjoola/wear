import SimpleHTTPServer
import SocketServer

import cgi
import json
import re
import sqlite3

from gcm import *
from secret import API_KEY

PORT = 3990
gcm = GCM(API_KEY)


def send_notification(user_id, requestor_name):
    """
    Sends a notification to the user at user_id that their location is being
    requested by requestor_name.

    user_id: User to send notification to.
    requestor_name: Name of the person requesting the location.
    """
    notification = { "contact_name": requestor_name }
    gcm.json_request(registration_ids=[user_id], data=notification)


"""
ShareWear server.
Handles requests from the ShareWear app. Stores data in a Sqlite database.
"""
class ShareWearServerHandler(SimpleHTTPServer.SimpleHTTPRequestHandler):

    # Connect to the ShareWear database.
    conn = sqlite3.connect('sharewear.db')

    # Database cursor.
    c = None

    def get_user(self, user_info):
        """
        Get the ID of the user with a given email or phone number.

        user_info: The user's email or phone number.
        returns: The user's ID, or None if it can't be found.
        """
        email = self.parse_email(user_info)
        phone = self.parse_phone(user_info)
        vals = self.c.execute(
            ("SELECT id FROM users WHERE email = '%s' " % email) +
            ("OR phone = '%s'" % phone)
        )
        ids = [x for x in vals]
        if len(ids) == 1:
            return str(ids[0][0])
        return None


    def get_name(self, id):
        """
        Gets the name of the user with a given ID.

        id: The user's ID.
        returns: The user's name, or None if it can't be found.
        """
        vals = self.c.execute("SELECT name FROM users WHERE id = '%s'" % id)
        names = [x for x in vals]
        if len(names) == 1:
            return str(names[0][0])
        return None


    def parse_phone(self, phone):
        """
        Parses a phone number and removes all extraneous characters. Leaves
        just the numbers.
        """
        return re.sub("[+\-\(\)\[\]\ ]", "", phone)


    def parse_email(self, email):
        """
        Parses an email and makes it into a storable form.
        """
        return email.lower()


    def send_error(self):
        """
        Send an error response.
        """
        self.send_response(422)


    def do_POST(self):
        """
        Handles a POST request from the ShareWear app.
        """
        # Initialize database cursor if needed.
        if self.c is None:
            self.c = self.conn.cursor()

        header = self.headers

        # Only handle JSON data. If it's not JSON, ignore it.
        content_type, _ = cgi.parse_header(header.getheader('Content-Type'))
        if content_type != 'application/json':
            self.send_response(400)
            return

        # Parse data into JSON.
        data = self.rfile.read(int(header.getheader('Content-Length')))
        json_data = json.loads(data)

        try:
            # Get the command and user ID.
            cmd = json_data["cmd"]
            id = json_data["id"]
            response = {}

            # New user. Insert details into database.
            if cmd == "new_user":
                name = json_data["name"]
                phone = self.parse_phone(json_data["phone"])
                email = self.parse_email(json_data["email"])
                self.c.execute("INSERT INTO users(id, name, phone, email) " +
                               "VALUES('%s', '%s', '%s', '%s')" %
                               (id, name, phone, email))

            # Location request from one user to another.
            elif cmd == "location_request":
                user_info = json_data["to"]
                other_id = self.get_user(user_info)
                requestor = self.get_name(id)

                # Could not find the user.
                if other_id is None or requestor is None:
                    self.send_error()
                    return

                send_notification(other_id, requestor)

            # Get location of user, if it exists.
            elif cmd == "location_get":
                user_info = json_data["to"]
                id = self.get_user(user_info)

                # Could not find user.
                if id is None:
                    self.send_error()
                    return

                locations = self.c.execute("SELECT lat, lng FROM locations WHERE " +
                                           ("locations.id = '%s' " % id) +
                                           "ORDER BY timestamp DESC LIMIT 1")
                loc_list = [x for x in locations]
                if len(loc_list) == 1:
                    lat = loc_list[0][0]
                    lng = loc_list[0][1]
                    response["lat"] = lat
                    response["lng"] = lng

                # No location found. Return default location indicating no
                # location.
                else:
                    response["lat"] = -1
                    response["lng"] = -1

            # User updating with their current location.
            elif cmd == "location_add":
                lat = float(json_data["lat"])
                lng = float(json_data["lng"])
                self.c.execute("INSERT INTO locations(id, timestamp, lat, " +
                               "lng) VALUES('%s', datetime('now'), '%s', '%s')"
                               % (id, lat, lng))

            # User clearing all location data.
            elif cmd == "location_clear":
                self.c.execute("DELETE FROM locations WHERE " +
                               "locations.id = '%s'" % id)

            # Commig database transaction.
            self.conn.commit()

            # Send response.
            self.send_response(200)
            self.end_headers()
            self.wfile.write(json.dumps(response))
            self.wfile.close()

        except Exception as e:
            import traceback
            traceback.print_exc()
            self.send_error()


# Start server.
handler = ShareWearServerHandler
httpd = SocketServer.TCPServer(("", PORT), handler)
print "Started server at", PORT
httpd.serve_forever()
