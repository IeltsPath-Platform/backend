# Phase 02. Dependency, cấu hình, Compose, production

[Plan](plan.md). Chỉ làm sau khi phase 01 được duyệt.

## File sửa

| File | Thay đổi |
| --- | --- |
| `services/learning-service/pom.xml` | Thêm `org.springframework.boot:spring-boot-starter-data-redis`, không ghi version (như community-service) |
| `services/learning-service/src/main/resources/application.yml` | `management.health.redis.enabled: false`, `spring.data.redis.repositories.enabled: false` |
| `infra/config-server/config-repo/learning-service.yaml` | Khối Redis và `learning.topic-cache` (dưới) |
| `docker-compose.yml` | `learning-service.environment`: `REDIS_HOST: redis`, `REDIS_PASSWORD: "${REDIS_PASSWORD}"` |
| `scripts/production/render-env.py` | Thêm `'learning-service'` vào tập `REDIS` (dòng 11) |
| `scripts/production/test-production.py` | Kiểm tra env của Learning có `REDIS_HOST`, `REDIS_PASSWORD`; Content và Assessment không có |

Hai khóa tắt health và repository để trong `application.yml` của module, không để ở config-repo, vì test context chạy không có
Config Server. File đó đã có khối `spring:`; ghép vào, không tạo khối thứ hai.

## Cấu hình config-repo

Ghép vào khối `spring:` và `learning:` đã có:

```yaml
spring:
  data:
    redis:
      host: ${REDIS_HOST:localhost}
      port: ${REDIS_PORT:6379}
      password: ${REDIS_PASSWORD}
      connect-timeout: 200ms
      timeout: 200ms

learning:
  topic-cache:
    # Display-only copy of Content's topic sequence. Off, or Redis down, reads go straight to Content.
    enabled: ${LEARNING_TOPIC_CACHE_ENABLED:true}
    ttl-seconds: 60
```

- Không thêm giá trị dự phòng cho `REDIS_PASSWORD` (quy tắc secret, `SecretConfigurationTest` kiểm tra).
- Hệ quả: thiếu `REDIS_PASSWORD` thì Learning không khởi động, kể cả khi tắt cache.
- Không khai báo property trùng tên biến môi trường (từng gây lỗi vòng placeholder `ACCESS_SERVICE_URL`).
- Không thêm `depends_on: redis` cho Learning: Redis là phần phụ.
- Công tắc phải tới được container, vì phase 05 dùng nó để lùi lại:
  - Compose: thêm `LEARNING_TOPIC_CACHE_ENABLED: "${LEARNING_TOPIC_CACHE_ENABLED:-true}"`;
  - production: thêm `LEARNING_TOPIC_CACHE_ENABLED` vào `OPTIONAL['learning']` trong `render-env.py` (dòng 22), vì script chỉ
    truyền các biến nằm trong danh sách đó.
- Không sửa `infra/redis/start-production.sh`, `maxmemory` hay chính sách evict.

## Kiểm tra

```powershell
mvn -q -pl infra/config-server test
mvn -q -pl services/learning-service -am -Dtest=LearningServiceApplicationTests -Dsurefire.failIfNoSpecifiedTests=false test
python scripts/production/test-production.py
docker compose config --quiet
```

Đạt khi: Learning khởi động được cả khi không có Redis, và `/actuator/health` vẫn `UP`.

## Lùi lại

Revert commit của phase. Không có dữ liệu cần dọn.
