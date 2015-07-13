import SimpleHTTPServer
import SocketServer

import cgi
import json

PORT = 3990

'''
Naive server. Keeps stuff in memory. TODO
'''
class ShareWearServerHandler(SimpleHTTPServer.SimpleHTTPRequestHandler):

    users = []

    def do_POST(self):
        header = self.headers

	# Only handle JSON data.
        content_type, _ = cgi.parse_header(header.getheader('Content-Type'))
        if content_type != 'application/json':
            self.send_response(400)
            return

        data = self.rfile.read(int(header.getheader('Content-Length')))
	json_data = json.loads(data)

        try:
	    cmd = json_data["cmd"]
            id = json_data["id"]
            response = {}

            # New user.
            if cmd == "new_user":
                name = json_data["name"]
                phone = json_data["phone"]
                email = json_data["email"]
                print "New user: ", name, phone, email, id
                # TODO
                self.users.append(json_data["name"])

            # Location request from one user to another.
            elif cmd == "location_request":
                user_to = json_data["to"]
		print "Location request: ",user_to
                # TODO

	    # Get location of user, if it exists.
            elif cmd == "location_get":
                user = json_data["to"] # TODO this is their email
                response["lat"] = 123.11
                response["lng"] = 23423.3
                print "Location get: ", user
                # TODO return 404 if not found, or respond with the current lat, lng of the user 

            # User updating with their current location.
            elif cmd == "location_add":
                lat = float(json_data["lat"])
		lng = float(json_data["lng"])
		print "Location add: ", lat, lng
                # TODO

            # User clearing all location data.
            elif cmd == "location_clear":
 		print "Location clear"
                # TODO

            self.send_response(200)
            self.end_headers()
            self.wfile.write(json.dumps(response))
            self.wfile.close()
            print "users now: " + str(self.users)

        except Exception as e:
            print "EXCEPTION"
            print e
            self.send_response(422)
            return

handler = ShareWearServerHandler
httpd = SocketServer.TCPServer(("", PORT), handler)

print "Started server at", PORT
httpd.serve_forever()

