# 第三方组件与许可 / Third-party components

知华科技（上海如静知华信息科技有限公司） · https://www.zhuatech.cn/ · 微信 zhuatech / zhuatech2

自有LICENSE不取代第三方许可。源码不把第三方库改署名；依赖通过官方包管理器取得，Docker镜像和依赖保持原许可与声明。精确版本见pom.xml、package-lock.json与Dockerfile。

The project license does not replace third-party terms. Libraries are obtained through official package managers and retain their notices/licenses. Exact versions are pinned in Maven/npm/Docker metadata.

| Component | Upstream / license |
|---|---|
| Spring Boot / Security | https://spring.io/ · Apache-2.0 |
| Hibernate ORM | https://hibernate.org/orm/ · LGPL-2.1 |
| Flyway Community | https://github.com/flyway/flyway · Apache-2.0 |
| Vue / Vite | https://github.com/vuejs/core / https://github.com/vitejs/vite · MIT |
| Lucide Vue | https://github.com/lucide-icons/lucide · ISC |
| MySQL / Connector-J | https://www.mysql.com/ · vendor GPL terms and Connector-J exceptions |
| Node / Nginx / Temurin | https://nodejs.org/ / https://nginx.org/ / https://adoptium.net/ · respective bundled licenses |

测试依赖JUnit、Mockito、H2、ESLint、Prettier、Black和各自依赖保持原许可，不作为知华原创源码。未内置外部供应商、支付、物流或模型SDK，也未分发商业字体。产品功能和第三方集成完成程度以README为准。

Test tooling retains its own licensing. No supplier/payment/carrier/model SDK or commercial font is bundled. Implemented capabilities/integration boundaries are stated in the READMEs.
