# RabbitMQ: vhost、用户和权限
rabbitmq中，vhost提供了资源隔离机制。同时，通过为用户设置vhost粒度的权限，可以实现丰富的权限控制功能。

## vhost
在rabbitmq中，可以创建被称为虚拟主机（vhost）的虚拟消费服务器。每个vhost逻辑上是一个独立的消息服务器，拥有自己独立的交换器、队列和绑定等资源。
vhost提供了隔离机制，使得用户可以在同一套rabbitmq服务器上创建多个vhost，并对不同对应用使用不同对vhost，不同的vhost之间相互隔离，这在业务复杂的大型应用中会比较有用。

RabbitMQ默认创建了名称为"/"的vhost，这也是大多数情况下默认使用的vhost，应用程序在此vhost中声明交换器、队列和绑定，供业务使用。
通过rabbitmqctl，可以方便的查看、创建和删除vhost。
### 查看vhost  
rabbitmqctl list_vhosts可以查看所有创建的vhost。
```
$ rabbitmqctl list_vhosts
Listing vhosts ...
/
```
### 创建vhost  
rabbitmqctl add_vhost {vhost}
```
$ rabbitmqctl add_vhost myVhost
Adding vhost "myVhost" ..
$ rabbitmqctl list_vhosts
Listing vhosts ...
/
myVhost
```
### 删除vhost  
rabbitmqctl delete_vhost {vhost}
```$xslt
$ rabbitmqctl delete_vhost myVhost
Deleting vhost "myVhost" ...
```

## 用户管理
### 创建用户    
rabbitmqctl add_user {username} {password}
```
$ add_user swaf swaf123
Adding user "swaf" ...

```
### 查看用户  
rabbitmqctl list_users
```
$ rabbitmqctl list_users
Listing users ...
swaf	[]
guest	[administrator]  //[]中为用户角色
```
### 修改用户密码  
rabbitmqctl change_password {username} {password} 
```
$ rabbitmqctl change_password swaf swaf456
Changing password for user "swaf" ...
```
### 验证用户
rabbitmqctl authenticate_user {username} {password} 
```
$ rabbitmqctl authenticate_user swaf swaf456
Authenticating user "swaf" ...
Success
$ rabbitmqctl authenticate_user swaf jaf
Authenticating user "swaf" ...
Error: failed to authenticate user "swaf"
user 'swaf' - invalid credentials
```
### 删除用户  
rabbitmqctl delete_user {username}
```
$ rabbitmqctl delete_user swaf
Deleting user "swaf" ...
```


## 权限控制
在RabbitMQ中，权限控制是以vhost为单位的。权限控制可以设置用户对某(几)个vhost内资源的访问权限。  
### 授权
为用户授权的命令为格式为: *rabbitmqctl set_permissions [-p vhost] {user} {conf}{write}{read}*  
其中
- -p 为可选项，指定vhost；
- user指定用户；
- {conf}{write}{read}分别指定用户可以在哪些资源上做配置、读、和写的权限。它们都是一个正则表达式，如果资源名称与指定的正则匹配，则用户对该资源有对
应对操作权限。需要说明对是{conf}是指可以对队列、交换器等做创建、删除之类等管理操作，{write}是指可以发布消息,{read}是指可以消费消息。  

```
# 创建一个vhost和user
$ rabbitmqctl add_vhost vh1
Adding vhost "vh1" ...

$ rabbitmqctl add_user u1 upass
Adding user "u1" ...

rabbitmqctl list_vhosts
Listing vhosts ...
vh1
/

$ rabbitmqctl list_users
Listing users ...
guest	[administrator]
u1	[]

# 授予用户权限
#1 授予用户u1对vh1上所有资源对可配置、可读、可写权限
$ rabbitmqctl set_permissions -p vh1 u1 ".*" ".*" ".*"
Setting permissions for user "u1" in vhost "vh1" ...

#2 用户u1对vh1上以error开头对资源有配置权限；对vh1上以log结尾对资源有写权限；对vh1上所有资源有读权限
$ rabbitmqctl set_permissions -p vh1 u1 "^error.*" ".*log$" ".*"
Setting permissions for user "u1" in vhost "vh1" ...

```

### 查看权限
有个两个命令可以查看用户权限，分别从vhost视角和user视角查看权限。  

- *rabbitmqctl list_permissions [-p vhost]* 查看vhost上对权限
- *rabbitmqctl list_user_permissions {username}* 查看用户对权限

```
$ rabbitmqctl list_permissions -p vh1
Listing permissions for vhost "vh1" ...
u1	^error.*	.*log$	.*

$ rabbitmqctl list_user_permissions u1
Listing permissions for user "u1" ...
vh1	^error.*	.*log$	.*
```

### 清除权限
与授权相同，清除用户权限也是以vhost为单位对，即一次性清除用户在某个vhost上设置对所有权限。  
清除权限命令为: *rabbitmqctl clear_permissions [-p vhost] {username}*  
```
$ rabbitmqctl clear_permissions -p vh1 u1
Clearing permissions for user "u1" in vhost "vh1" ...
Tswaf:~ wangtong$ rabbitmqctl list_user_permissions u1
Listing permissions for user "u1" ...

$ 
```

