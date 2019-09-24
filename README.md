## 微服务框架部署指南
#### 目录
- 1. [management类型框架部署(v1.4.1)](#1)
- 2. [share类型框架部署(v1.3.2)](#2)
- 3. [dedicate类型框架部署(v1.4.0)](#3)

#### 1. management类型框架部署(v1.4.1)<a name="1"></a>
##### 部署前准备
* Kubernetes集群环境
    AKS-Management-Cluster: 1.14.6, 需要安装helm-tiller，配置一个`public IP`
    AKS-User-Cluster: 1.14.6, 需要安装helm-tiller
    集群间可内网通信
* 本地运行环境信息（自测环境）
    Ubuntu 18.04.6
    kubectl 1.13.4
    helm 2.12.1

* 需要安装有python包：yaml、jinja2，安装命令如下：

```
# apt update
# apt install python-yaml
# apt install python-jinja2
```
* 获取部署脚本

```
# mkdir -p /workspace/script
# git clone git@advgitlab.eastasia.cloudapp.azure.com:micro-services/frame_deploy_for_helm.git /workspace/script/
# cd /workspace/script
# git submodule init
# git submodule update
```
更新子仓库，进入相关模块执行：

```
# cd /workspace/script/service/third-party/consul-server
# git git pull origin master
```
需要更新的模块有consul-server，dnsmasq，nginx7，kong，依次更新

##### 1.1 修改配置文件
打开全局变量配置文件：
```
# vi /workspace/script/global_values.yaml
```
version: 微服务框架版本号，目录中已经注明

namespace: 部署slave时框架组件所在的命名空间

image.dockerRegistry: 仓库地址，举例，如果仓库是harbor.wise-paas.io，项目名是microservice,此处填写harbor.wise-paas.io/microservice

image.tag: 镜像版本号

consulToken: consul-server的token，用于UI界面访问和服务挂载

domian: `public IP`所绑定的域名，也是外部访问服务的url后缀

externalIP: 即`public IP`

managementDnsInternalIP: aks(master)内网网段内未被使用的某个ip，用于集群间dns信息共享

managementDnsRegisterInternalIP: aks(master)内网网段内未被使用的某个ip，用于slave向master注册集群信息

kongInternalIP: aks(slave)内网网段内未被使用的某个ip，用于集群内部访问

clusterName: aks(slave)对应的集群名

registryPassinfoBase64: 仓库认证信息，获取方式见注释

pem: 域名对应的https证书文件内容

key: 域名对应的https秘钥文件内容

以上就是整个部署流程中用到的所有全局变量，某些变量也决定了最后的服务访问方式：
```
http://<service-name>-<namespace>-<clusterName>-dc.<domain>
https://<service-name>-<namespace>-<clusterName>-dc.<domain>
```
其中`namespace`和`clusterName`中切记不要带`-`
##### 1.2 master部署
将/root/.kube/config文件替换为master集群对应的config文件

制作chart包:
```
# mkdir -p /workspace/script
# cd /workspace/script
# ./misctl.py config -t management -m kong -r master
```
执行后能在/workspace/script/tmp/路径下看到chart包

部署master上的组件:
```
# ./misctl.py install -t management -m kong -r master
```
配置coredns:
```
# vi /workspace/master-coredns.yaml
```
替换`<managementDnsInternalIP>`，写入以下内容:
```
apiVersion: v1
kind: ConfigMap
metadata:
  name: coredns-custom
  namespace: kube-system
data:
  test.server: |
    wp-service:53 {
        errors
        cache 1
        proxy . <managementDnsInternalIP>
    }
```
更新配置:
```
# kubectl apply -f /workspace/master-coredns.yaml
```
重启coredns pod:
```
# kubectl delete pod -n kube-system -l k8s-app=kube-dns
```
master部署完毕
##### 1.3 slave部署
将/root/.kube/config文件替换为slave集群对应的config文件

制作chart包:
```
# mkdir -p /workspace/script
# cd /workspace/script
# ./misctl.py config -t management -m kong -r slave
```
执行后能在/workspace/script/tmp/路径下看到chart包

部署slave上的组件:
```
# ./misctl.py install -t management -m kong -r slave
```
配置coredns:
```
# vi /workspace/slave-coredns.yaml
```
替换`<managementDnsInternalIP>`和`<dnsmasqServiceIP>`，写入以下内容:

`<dnsmasqServiceIP>`获取方式如下：
```
# kubectl get svc dnsmasq-service -n <namespace> -o jsonpath='{.spec.clusterIP}'
```
```
apiVersion: v1
kind: ConfigMap
metadata:
  name: coredns-custom
  namespace: kube-system
data:
  test.server: |
    wp-service:53 {
        errors
        cache 1
        proxy . <managementDnsInternalIP>
    }
    service.frame:53 {
        errors
        cache 1
        proxy . <dnsmasqServiceIP>
    }
```
更新配置:
```
# kubectl apply -f /workspace/slave-coredns.yaml
```
重启coredns pod:
```
# kubectl delete pod -n kube-system -l k8s-app=kube-dns
```
slave部署完毕
