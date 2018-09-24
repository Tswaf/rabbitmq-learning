# RabbitMQ集群部署

## 单节点部署
rabbitmq单节点部署比较简单，可以使用apt-get等工具快速安装部署。

```
wget -O- https://www.rabbitmq.com/rabbitmq-release-signing-key.asc | sudo apt-key add -
echo 'deb http://www.rabbitmq.com/debian/ {distriubtion} main' | sudo tee /etc/apt/sources.list.d/rabbitmq.list
sudo apt-get update
sudo apt-get install rabbitmq-server
```
如上，添加仓库等认证key，更新本地仓库后安装即可。其中{distriubtion}需更加不同的系统版本指定。  
使用apt-get等工具安装比较快速，会一次性安装erlang等依赖。也可以手动安装erlang后，在安装rabbitmq，这种方式也无多大成本，下载rabbitmq的tar.gz包
后解压即可。   
rabbitmq安装后，以下为几个比较重要的目录，根据不同的系统和安装方式，目录路径会有所差别：    
- {rabbitHome}/etc: 配置文件目录，rabbitmq.config文件定义rabbitmq配置
- {rabbitHome}/sbin：rabbitmq可执行文件目录，如：rabbitmq-server、rabbitmqctl、rabbitmq-plugins
- {rabbitHome}/plugins: rabbitmq内置的插件（.ez）放在此目录，开启相应的插件即可使用。
- {rabbitHome}/var/log：日志目录

下载后使用 *rabbitmq-server \[- detached]* 即可启动rabbitmq服务。
![](img/cluster/rabbit-single-node.png)

后台首先会启动一个erlang虚拟机机，然后在此虚拟机中启动rabbitmq服务。  

## 集群部署
rabbitmq提供了多种集群部署的方式：  
- 静态配置：在rabbit.conf文件中静态配置集群节点
- rabbitmqctl: 使用rabbitmqctl工具手动组装集群
- 服务发现：基于etcd、consul、k8s等动态配置集群

这里主要介绍使用rabbitmqctl组集群等方式，这种方式在小规模下灵活高效。  
假设局域网中有node1,node2,node3三个节点，配置rabbitmq集群需要以下几步：
1. 复制erlang cookie
    如上所述，rabbitmq允许在erlang虚拟机中，erlang节点需要更加erlang cookie通信，为此需要保证三个节点上等cookie一致。cookie默认位于*/var/lib/rabbitmq/.erlang.cookie*
    目录下，从一个节点上复制拷贝到其他节点即可。
    
    ```
    root@node1:~# cat /var/lib/rabbitmq/.erlang.cookie 
    JZQIKYDOSOKMADVDSPUO
    
    root@node2:~# echo JZQIKYDOSOKMADVDSPUO > /var/lib/rabbitmq/.erlang.cookie
    root@node3:~# echo JZQIKYDOSOKMADVDSPUO > /var/lib/rabbitmq/.erlang.cookie
    ```
2. 分别启动节点
    
    ```
    root@node1:~# rabbitmq-server -detached
    Warning: PID file not written; -detached was passed.
    
    root@node2:~# rabbitmq-server -detached
    Warning: PID file not written; -detached was passed.
    
    root@node3:~# rabbitmq-server -detached
    Warning: PID file not written; -detached was passed.
    ```
    在三个节点上分别启动rabbit，此时三个rabbitmq各自独立。可以使用*rabbitmqctl cluster_status*命令分别在三个节点上查看集群状态。  
    
    ```
    root@node1:~# rabbitmqctl cluster_status
    Cluster status of node rabbit@node1
    [{nodes,[{disc,[rabbit@node1]}]},
     {running_nodes,[rabbit@node1]},
     {cluster_name,<<"rabbit@node1">>},
     {partitions,[]},
     {alarms,[{rabbit@node1,[]}]}]
    ```

3. 加入集群
    以node1为基础，将node2和node3加入集群。
    ```
    root@node2:~# rabbitmqctl stop_app
    Stopping rabbit application on node rabbit@node2
    root@node2:~# rabbitmqctl reset
    Resetting node rabbit@node2
    root@node2:~# rabbitmqctl join_cluster rabbit@node1
    Clustering node rabbit@node2 with rabbit@node1
    root@node2:~# rabbitmqctl start_app
    Starting node rabbit@node2
    root@node2:~#
    ```
    首先*rabbitmqctl stop_app*会停止rabbitmq服务，然后*rabbitmqctl reset*会重置集群状态，接着将node2加入node1所在集群，最后在启动node2上
    到rabbitmq服务。node3上执行同样到操作。  
    完成后，三个节点已经组成回集群，再次使用*rabbitmqctl cluster_status*命令任意节点上查看集群状态：
    
    ```
    root@node3:~# rabbitmqctl cluster_status
    Cluster status of node rabbit@node3
    [{nodes,[{disc,[rabbit@node1,rabbit@node2,rabbit@node3]}]},
     {running_nodes,[rabbit@node1,rabbit@node2,rabbit@node3]},
     {cluster_name,<<"rabbit@node1">>},
     {partitions,[]},
     {alarms,[{rabbit@node1,[]},{rabbit@node2,[]},{rabbit@node3,[]}]}]
    root@node3:~# 
    ```

到此集群即组建完毕，组建过程并不复杂。   

后续如果需要动态将当个节点脱离集群，也很容易：
```
root@node3:~# rabbitmqctl stop_app
Stopping rabbit application on node rabbit@node3
root@node3:~# rabbitmqctl reset
Resetting node rabbit@node3
root@node3:~# rabbitmqctl start_app
Starting node rabbit@node3
root@node3:~#
```
关闭节点上到rabbitmq服务，重置该节点集群状态，重启rabbitmq服务即可。


## 集群节点类型
上面在使用*rabbitmqctl cluster_status*查看集群状态时，会发现node列表中的disc，这是表明节点类型的。  
在集群中，存在两种节点：  
- disc 磁盘节点，元数据存储到磁盘
- ram 内存节点，元数据存储到磁盘

磁盘节点将队列、交换器、绑定关系等源数据存储到磁盘中，而内存节点将这些数据存储在内存中。
上面组建集群时，节点默认为disc类型，也可以在将节点加入集群时指定为ram类型：
```
root@node2:~# rabbitmqctl join_cluster rabbit@node1 --ram
```
对于集群中已有等节点，也可以改变其节点类型：
```
root@node2:~# rabbitmqctl change_cluster_node_typef {disc,ram}
```

当创建一个队列、交换器、绑定等时，需要服务端各个节点将源数据写入后才会响应客户端，这意味这磁盘节点会具有较慢的响应。在频繁创建队列、交换器等创建中，
磁盘节点会拖慢集群响应速度。不过实际中，频繁创建元数据等场景并不多见，所以可以一般将集群中节点全部设置成磁盘节点。在rpc等频繁创建元数据等场景下，
可以考虑使用内存节点。   
在一个集群中，应该保证至少有一个磁盘节点，这样才能保证集群重启后元数据等恢复。  

