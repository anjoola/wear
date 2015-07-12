'''

Courtesy of https://www.digitalocean.com/community/tutorials/how-to-create-a-server-to-send-push-notifications-with-gcm-to-android-devices-using-python
'''
from gcm import *
from secret import *

gcm = GCM(API_KEY)

# TODO translate ID to contact name
contact_name = "TestUser"
data = { 'contact_name': contact_name }

reg_id = REG_ID

reg_ids = [reg_id]
reponse = ""
try:
	response = gcm.json_request(registration_ids=reg_ids, data=data)
except e:
	print e

print response
