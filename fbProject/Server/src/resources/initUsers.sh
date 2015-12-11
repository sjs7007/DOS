#create User
curl -H "Content-Type: application/json" -X POST -d '{"Email" : "sjs7007" , "Name" : "Saravanan","Birthday": "26th July 1994","CurrentCity" : "Gainesville"}' http://localhost:8087/createUser
echo
curl -H "Content-Type: application/json" -X POST -d '{"Email" : "sigmoid" , "Name" : "kurkure","Birthday": "26th Sep 1956","CurrentCity" : "Gainesville"}' http://localhost:8087/createUser
echo

#make posts
curl -H "Content-Type: application/json" -X POST -d '{"fromEmail" : "sjs7007","toEmail" : "sjs7007","data":"my post so cooooool."}' http://localhost:8087/wallWrite
echo
curl -H "Content-Type: application/json" -X POST -d '{"fromEmail" : "sigmoid","toEmail" : "sigmoid","data":"my post so  much cooooooler."}' http://localhost:8087/wallWrite
echo
curl -H "Content-Type: application/json" -X POST -d '{"fromEmail" : "sjs7007","toEmail" : "sigmoid","data":"my post so cooooool."}' http://localhost:8087/wallWrite
echo 

#send friend request
curl -H "Content-Type: application/json" -X POST -d '{"fromEmail" : "sjs7007","toEmail" : "sigmoid"}' http://localhost:8087/sendFriendRequest
echo 
curl -H "Content-Type: application/json" -X POST -d '{"fromEmail" : "sigmoid","toEmail" : "sjs7007"}' http://localhost:8087/sendFriendRequest
echo 
curl -H "Content-Type: application/json" -X POST -d '{"fromEmail" : "sjs7007","toEmail" : "Do8Mufasa@gmail.com"}' http://localhost:8087/sendFriendRequest
echo 

#create album
curl -H  "Content-Type: application/json" -X POST -d '{"Email" : "sjs7007","Title" : "dis mah album"}' http://localhost:8087/createAlbum
echo  

#get list of albumids of user
echo "album ids of sjs7007"
curl http://localhost:8087/users/sjs7007/albums
echo

#upload image to specific albumid
curl -i -F filedata=@doge.jpg http://localhost:8087/users/sjs7007/albums/1/uploadCurl

#create page 
curl -H "Content-Type: application/json" -X POST -d '{"adminEmail" : "sjs7007","Title" : "4chan the haxxxor","pageID":"tempPAGEID"}' http://localhost:8087/createPage
echo 

#follow page
curl -H "Content-Type: application/json" -X POST -d '{"Email" : "sjs7007"}' http://localhost:8087/pages/tempPAGEID/follow
echo 

#make a post on page
curl -H "Content-Type: application/json" -X POST -d '{"fromEmail" : "sjs7007","data" : "post on the haxxxor page"}' http://localhost:8087/pages/tempPAGEID/createPost
echo 

#view posts of sjs7007
echo "Posts of sjs7007"
curl http://0.0.0.0:8087/users/sjs7007/posts?Email=sjs7007
echo

#viewposts of sifmoid
echo "Posts of sigmoid"
curl http://0.0.0.0:8087/users/sigmoid/posts?Email=sigmoid
echo

#view users
echo "Users Registered."
curl  http://0.0.0.0:8087/userList
echo

#view page directory
echo "Page directory."
curl http://0.0.0.0:8087/pageDirectory
echo 

#get a list of post ids on sjs7007
echo "Post ids"
curl http://localhost:8087/users/sjs7007/posts/ids?Email=sjs7007
echo 

#get specific post
#curl http://localhost:8087/user