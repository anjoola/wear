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

            # New user.
            if cmd == "new_user":
                # TODO
                self.users.append(json_data["name"])

            # Location request from one user to another.
            elif cmd == "location_request":
                user_from = json_data["id"]
                user_to = json_data["to"]
                # TODO

            # User updating with their current location.
            elif cmd == "location_add":
                user = json_data["id"]
                lat = float(json_data["lat"])
		lng = float(json_data["lng"])
                # TODO

            # User clearing all location data.
            elif cmd == "location_clear":
                user = json_data["id"]
                # TODO

            self.send_response(200)
            print "users now: " + str(self.users)

        except Exception as e:
            print e
            self.send_response(422)
            return

handler = ShareWearServerHandler
httpd = SocketServer.TCPServer(("", PORT), handler)

print "Started server at", PORT
httpd.serve_forever()

