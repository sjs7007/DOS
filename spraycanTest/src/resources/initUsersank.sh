#create User
curl -H "Content-Type: application/json" -X POST -d '{"Email" : "sjs7007" , "Name" : "Saravanan","Birthday": "26th July 1994","CurrentCity" : "Gainesville"}' http://192.168.0.21:5020/createUser
echo
curl -H "Content-Type: application/json" -X POST -d '{"Email" : "sigmoid" , "Name" : "kurkure","Birthday": "26th Sep 1956","CurrentCity" : "Gainesville"}' http://192.168.0.21:5020/createUser
echo

#make posts
curl -H "Content-Type: application/json" -X POST -d '{"fromEmail" : "sjs7007","toEmail" : "sjs7007","data":"my post so cooooool.","postID" : "tempPostID"}' http://192.168.0.21:5020/wallWrite
echo
curl -H "Content-Type: application/json" -X POST -d '{"fromEmail" : "sigmoid","toEmail" : "sigmoid","data":"my post so  much cooooooler."}' http://192.168.0.21:5020/wallWrite
echo
curl -H "Content-Type: application/json" -X POST -d '{"fromEmail" : "sjs7007","toEmail" : "sigmoid","data":"my post so cooooool.","postID" : "tempPostID"}' http://192.168.0.21:5020/wallWrite
echo 

#send friend request
curl -H "Content-Type: application/json" -X POST -d '{"fromEmail" : "sjs7007","toEmail" : "sigmoid"}' http://localhost:8087/sendFriendRequest
echo 
curl -H "Content-Type: application/json" -X POST -d '{"fromEmail" : "sjs7007","toEmail" : "Do8Mufasa@gmail.com"}' http://localhost:8087/sendFriendRequest
echo 

#create page 
curl -H "Content-Type: application/json" -X POST -d '{"adminEmail" : "sjs7007","Title" : "4chan the haxxxor","pageID":"tempPAGEID"}' http://192.168.0.21:5020/createPage
echo 

#follow page
curl -H "Content-Type: application/json" -X POST -d '{"Email" : "sjs7007"}' http://192.168.0.21:5020/pages/tempPAGEID/follow
echo 

#make a post on page
curl -H "Content-Type: application/json" -X POST -d '{"fromEmail" : "sjs7007","postID" : "tempPostID","data" : "post on the haxxxor page"}' http://192.168.0.21:5020/pages/tempPAGEID/createPost
echo 

#view posts of sjs7007
echo "Posts of sjs7007"
curl http://192.168.0.21:5020/user/sjs7007/posts?Email=sjs7007
echo

#viewposts of sifmoid
echo "Posts of sigmoid"
curl http://192.168.0.21:5020/user/sigmoid/posts?Email=sigmoid
echo

#view users
echo "Users Registered."
curl  http://192.168.0.21:5020/usersList
echo

#view page directory
echo "Page directory."
curl http://192.168.0.21:5020/pageDirectory
echo 