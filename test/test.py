import requests


data = {
    'key': "aaa",
    'value': 1,
    'writeQuorum' : 3
}
r = requests.post('http://192.168.99.101:8080/save', params=data).json()

print r




data = {
    'key': "aaa",
    'value': 1,
    'writeQuorum' : 3
}
r = requests.post('http://192.168.99.101:8080/write', params=data).json()