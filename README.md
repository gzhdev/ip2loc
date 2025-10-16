# IP Geolocation Service

基于 Spring Boot 的 IP 地址定位服务，使用 [IP2Location](https://www.ip2location.com/) 数据库进行 IP 地址解析。

## 功能特性

- `GET /api/location?ip=203.0.113.1`：通过查询参数传入 IP 地址并返回定位信息。
- `GET /api/location`：未提供查询参数时，自动使用访问者的 IP 地址进行定位。
- 自动下载并定期更新 IP2Location 数据库（需要配置 `ip2location_token`）。
- 返回国家、省份、城市、经纬度、ISP 等常用字段。

## 运行要求

- Java 17+
- Maven 3.9+
- 可访问互联网以下载 IP2Location 数据文件

## 配置

应用使用 `application.yml` 文件中的配置项。

```yaml
ip2location:
  token: ${ip2location_token:${IP2LOCATION_TOKEN:}}
  edition: DB11LITEBIN
  database-path: data/IP2LOCATION-LITE-DB11.BIN
  update-interval: P1D
```

1. 注册 IP2Location 账号后获取 `ip2location_token`。
2. 在运行环境中设置环境变量 `ip2location_token` 或 `IP2LOCATION_TOKEN`。
3. 首次启动时应用会自动下载 ZIP 文件并解压为二进制数据库。
4. 默认每天更新一次，可通过 `update-interval` 修改刷新频率（ISO-8601 Duration）。

> **提示**：下载行为需要外网访问权限；若缺少 token，将跳过下载，REST API 会在数据库不可用时返回 404。

## 构建与运行

```bash
mvn spring-boot:run
```

启动后可使用 `curl` 访问：

```bash
curl "http://localhost:8080/api/location?ip=8.8.8.8"
```

## 测试

项目包含基于 JUnit 5 与 Spring MockMvc 的单元测试，可通过以下命令运行：

```bash
mvn test
```

## 许可证

本项目基于 [MIT License](LICENSE)。
