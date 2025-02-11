# Chlorophyll
[![Issues](https://img.shields.io/github/issues/EilsapMC/Chlorophyll?style=flat-square)](https://github.com/EilsapMC/Chloriphyll/issues)
![Commit Activity](https://img.shields.io/github/commit-activity/w/EilsapMC/Chlorophyll?style=flat-square)
![CodeFactor Grade](https://img.shields.io/codefactor/grade/github/EilsapMC/Chlorophyll?style=flat-square)
![GitHub Repo stars](https://img.shields.io/github/stars/EilsapMC/Chlorophyll?style=flat-square)
![GitHub License](https://img.shields.io/github/license/EilsapMC/Chlorophyll)

## 它做了什么？
该模组将每个世界包括世界上的其他杂物的tick迁移到了独立的tickloop中,使得每个世界包括原有的Server Thread之间几乎互不影响(除了传送门搜索的忙等待之外),
该模组旨在为有一些fabric模组空岛服提供一个适合的调度优化使得其能够在单个服务端承载更多玩家

## 它是怎么工作的
调度: 由于每个世界都有其独立的ticklist,所以我们并没有像folia那样自己制造一个容器而是借助于原有的ticklist进行改造,我们将原有的主线程调度中的拉取区块任务的删除让其在
世界的tickloop中进行这一步,而Server Thread则仅负责GUI更新和新玩家的连接(到Configure完成这一阶段)</br>

刻循环: 这里我们参考了folia的做法,但是我们并没有实现自己的线程池而是借助了java自带的,每个世界刻循环实现了Runnable,我们在每次tick完成时会去计算到下一刻的延迟
然后将其重新按照该延迟丢入线程池形成刻循环,在自身线程内的数据沟通我们并没用使用ThreadLocal而是通过继承线程本身通过Thread.currentThread()传递对象

## 参考
我们向我们参考的各种项目做出真诚的感谢，没有这些项目的经验，我们就无法做出这样的模组
 
- [Folia](https://github.com/PaperMC/Folia)
- [SparklyPaper](https://github.com/SparklyPower/SparklyPaper)
- [DimensionalThreading](https://github.com/WearBlackAllDay/DimensionalThreading)
- [Luminol](https://github.com/LuminolMC/Luminol)

## 下载

任何构建可以在Action中找到 

<b>注意: 该模组还属于早期测试阶段,请使用前谨慎考虑,由于该模组造成的任何损失我们概不负责</br>
