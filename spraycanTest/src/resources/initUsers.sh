#create User
curl -H "Content-Type: application/json" -X POST -d '{"Email" : "sjs7007" , "Name" : "Saravanan","Birthday": "26th July 1994","CurrentCity" : "Gainesville"}' http://localhost:8087/createUser
echo
curl -H "Content-Type: application/json" -X POST -d '{"Email" : "sigmoid" , "Name" : "kurkure","Birthday": "26th Sep 1956","CurrentCity" : "Gainesville"}' http://localhost:8087/createUser
echo

#make posts
curl -H "Content-Type: application/json" -X POST -d '{"fromEmail" : "sjs7007","toEmail" : "sjs7007","data":"my post so cooooool.","postID" : "100"}' http://localhost:8087/wallWrite
echo
curl -H "Content-Type: application/json" -X POST -d '{"fromEmail" : "sigmoid","toEmail" : "sigmoid","data":"my post so  much cooooooler."}' http://localhost:8087/wallWrite
echo
curl -H "Content-Type: application/json" -X POST -d '{"fromEmail" : "sjs7007","toEmail" : "sigmoid","data":"my post so cooooool.","postID" : "100"}' http://localhost:8087/wallWrite
echo 

#send friend request from sjs7007 to sigmoid

#create page 
curl -H "Content-Type: application/json" -X POST -d '{"adminEmail" : "sjs7007","Title" : "4chan the haxxxor","pageID":"tempID"}' http://localhost:8087/createPage
echo 

#view posts of sjs7007
echo "Posts of sjs7007"
curl http://0.0.0.0:8087/user/sjs7007/posts?Email=sjs7007
echo

#viewposts of sifmoid
echo "Posts of sigmoid"
curl http://0.0.0.0:8087/user/sigmoid/posts?Email=sigmoid
echo

#view users
echo "Users Registered."
curl  http://0.0.0.0:8087/users
echo

