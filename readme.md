#### 工作流（flowable)

部署方法请参见说明工程：https://github.com/blueskylong/aolie_illustrate

工作流使用的flowable6.5.0引擎,并集成了前端工程，部署时，可以将resources/static目录 抽出和前端部署在一起，也可以前后端都部署在一起。
修改了编辑器的角色和人员的查询接口，以适合本系统。

本插件有"流程管理" 和"业务表对应流程"二个功能.另外每一个附加了流程的表格都会有附加流程状态列.
每一张业务表，都可以关联一个流程。流程的接入是插件式的，不需要修改原来的业务方案设计，流程的引入和去除，不影响原表的结构。通过拦截器，动态增加了流程的控制信息，及流程状态列。
表增加流程后，前端会动态增加流程的列，可通过增加按钮，实现查看流程节点状态信息。

