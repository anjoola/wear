import SimpleHTTPServer
import SocketServer

import cgi
import json
import sqlite3

from gcm import *
from secret import *

PORT = 3990
gcm = GCM(API_KEY)

def send_notification(user_id, requestor_name):
    """
    Sends a notification to the user at user_id that their location is being
    requested by user_name.

    user_id: User to send notification to.
    requestor_name: Name of the person requesting the location.
    """
    notification = {}
    notification["contact_name"] = requestor_name
    gcm.json_request(registration_ids=[user_id], data=notififcation)


'''
Naive server. Keeps stuff in memory. TODO
'''
class ShareWearServerHandler(SimpleHTTPServer.SimpleHTTPRequestHandler):

    conn = sqlite3.connect('sharewear.db')
    c = None

    def get_user(self, email):
        """
        Get the ID of the user with a given email.

        email: The user's email.
        returns: Their ID.
        """
        vals = self.c.execute("SELECT id FROM users WHERE email LIKE '%s'"
                              % email)
        ids = [x for x in vals]
        if len(ids) == 1:
            return ids[0][0]
        return None


    def get_name(self, id):
        """
        Gets the name of the user with a given ID.

        id: The user's ID.
        returns: The user's name.
        """
        vals = self.c.execute("SELECT name FROM users WHERE id = '%s'" % id)
        names = [x for x in vals]
        if len(names) == 1:
            return names[0][0]
        return None


    def do_POST(self):
        if self.c is None:
            self.c = self.conn.cursor()

        header = self.headers

	# Only handle JSON data.
        content_type, _ = cgi.parse_header(header.getheader('Content-Type'))
        if content_type != 'application/json':
            self.send_response(400)
            return

        data = self.rfile.read(int(header.getheader('Content-Length')))
	json_data = json.loads(data)

	# TODO handle errors (if user is none, etc.)

        try:
	    cmd = json_data["cmd"]
            id = json_data["id"]
            response = {}

            # New user.
            if cmd == "new_user":
                name = json_data["name"]
                phone = json_data["phone"]
                email = json_data["email"]
                print "---New user: ", name, phone, email, id
                self.c.execute("INSERT INTO users(id, name, phone, email) " +
                               "VALUES('%s', '%s', '%s', '%s')" %
                               (id, name, phone, email))

            # Location request from one user to another.
            elif cmd == "location_request":
                user_email = json_data["to"]
		print "---Location request: ", user_email
                other_id = self.get_user(user_email)
                requestor = self.get_name(id)
                send_notification(other_id, requestor)

	    # Get location of user, if it exists.
            elif cmd == "location_get":
                user_email = json_data["to"]
                print "---Location get: ", user_email
                id = self.get_user(user_email)
                locations = self.c.execute("SELECT lat, lng FROM locations WHERE " +
                                           "locations.id = '%s' ORDER BY" +
                                           "TIMESTAMP DESC LIMIT 1" % id)
                loc_list = [x for x in locations]
                if len(loc_list) == 1:
                    lat = loc_list[0][0]
                    lng = loc_list[0][1]
                    response["lat"] = lat
                    response["lng"] = lng
                else:
		    pass # TODO
                # TODO return 404 if not found, or respond with the current lat, lng of the user 

            # User updating with their current location.
            elif cmd == "location_add":
                lat = float(json_data["lat"])
		lng = float(json_data["lng"])
		print "---Location add: ", lat, lng
                self.c.execute("INSERT INTO locations(id, timestamp, lat, " +
                               "lng) VALUES('%s', datetime('now'), '%s', '%s')"
                               % (id, lat, lng))

            # User clearing all location data.
            elif cmd == "location_clear":
 		print "---Location clear"
                self.c.execute("DELETE FROM locations WHERE locations.id = '%s'" % id)

            self.conn.commit()
            self.send_response(200)
            self.end_headers()
            self.wfile.write(json.dumps(response))
            self.wfile.close()

        except Exception as e:
            print "EXCEPTION:", e
            self.send_response(422)
            return

handler = ShareWearServerHandler
httpd = SocketServer.TCPServer(("", PORT), handler)

print "Started server at", PORT
httpd.serve_forever()

