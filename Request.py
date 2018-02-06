import json, requests

citylist = []
with open('Cities') as inputfile:
    for line in inputfile:
        citylist.append(line)
f = open('coordinates.txt','w')
for location in range(len(citylist)):
    resp = requests.get(url = "https://maps.googleapis.com/maps/api/geocode/json?address=" + citylist[location] + "&key=AIzaSyAHTmTxlkasYVxQcFNKyhxb4JEuQ5oJSn4")
    data = json.loads(resp.content)
    latitude = data["results"][0]["geometry"]["location"]["lat"]
    longitude = data["results"][0]["geometry"]["location"]["lng"]
    f.write(str(latitude) + "," + str(longitude) + "#" + "\n")
    print(str(location+1) + "  " + citylist[location] + " " + str(latitude) + "," + str(longitude) + "#")
f.close()





