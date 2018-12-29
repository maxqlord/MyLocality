import json, requests

citylist = []
with open('Cities') as inputfile:
    for line in inputfile:
        citylist.append(line)

for location in range(500, len(citylist)):
    resp = requests.get(url = "https://maps.googleapis.com/maps/api/geocode/json?address=" + citylist[location] + "&key=AIzaSyCIRySZkmd2skHCd1QZtOtJgTB8JNn5cOA")
    data = json.loads(resp.content)
    latitude = data["results"][0]["geometry"]["location"]["lat"]
    longitude = data["results"][0]["geometry"]["location"]["lng"]
    print(str(latitude) + "," + str(longitude) + "#")
                    #print(str(location+1) + "  " + citylist[location] + " " + str(latitude) + "," + str(longitude) + "#")





