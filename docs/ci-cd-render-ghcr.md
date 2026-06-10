# CI/CD (GitHub Actions → Render)

## Mục tiêu
- CI: chạy test và build Maven khi PR/push.
- CD (main): chạy test và trigger Render deploy hook (Render tự build bằng Dockerfile trong repo).

## Workflow
- CI: `.github/workflows/ci.yml`
- CD: `.github/workflows/cd.yml`

## Cấu hình GitHub
### Secrets
- `RENDER_DEPLOY_HOOK_URL`: Deploy Hook URL của Render service.

## Cấu hình Render
### Chọn kiểu deploy
- Dùng Web Service kiểu Docker (Render build từ Dockerfile trong repo).
### Auto deploy
- Tắt Auto Deploy (khuyến nghị) và chỉ deploy qua Deploy Hook để đảm bảo chỉ deploy khi CI pass.

## Env vars tối thiểu (Render)
- `DATABASE_URL`
- `DATABASE_USERNAME`
- `DATABASE_PASSWORD`
- `JWT_SECRET`
- `APP_BASE_URL`
- `FRONTEND_URL`
- `CORS_ALLOWED_ORIGINS`
- Nếu dùng email: `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD`
