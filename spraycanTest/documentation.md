# Paths

+ /users/
Returns list of all users registered.
Type : GET 
Parameters : None

+ /createUser/
Allows to register user on fb.
Type : POST 
Parameters : Email,Name,Birthday,CurrentCity

+ /sendFriendRequest/
Allows to send friend requests.
Type : POST
Parameters : fromEmail,toEmail

+ /user/<Email>/friends
Returns list of friends.
Type : GET 
Parameters : None

+ /user/<Email>/profile
Return user profile.
Type : GET 
Parameters : None

+ /user/<Email>/posts
Returns list of posts by specific user.
Authorization : should be friend or same person to view posts.
Type : GET
Parameters : fromEmail

+ /user/<Email>/posts/<postID>
Returns post specified by post ID.
Authorization : should be friend or same person to view posts.
Type : GET 
Paremeters : fromEmail 

+ /wallWrite
Make a fb post on someone's wall. 
Authorization : should be friend or same person to make posts.
Type : POST 
Parameters : fromEmail,toEmail,data,postID
