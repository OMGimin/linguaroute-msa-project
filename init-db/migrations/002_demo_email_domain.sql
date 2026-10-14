-- 기존 볼륨의 데모 계정 도메인을 scala-tech.local에서 skala-tech.local로 변경한다.
-- 새 이메일이 이미 있으면 UNIQUE 충돌을 피하기 위해 기존 행을 그대로 둔다.

UPDATE users
SET email = 'admin@skala-tech.local'
WHERE email = 'admin@scala-tech.local'
  AND NOT EXISTS (
      SELECT 1 FROM (SELECT email FROM users) existing_users
      WHERE existing_users.email = 'admin@skala-tech.local'
  );

UPDATE users
SET email = 'employee.lee@skala-tech.local'
WHERE email = 'employee.lee@scala-tech.local'
  AND NOT EXISTS (
      SELECT 1 FROM (SELECT email FROM users) existing_users
      WHERE existing_users.email = 'employee.lee@skala-tech.local'
  );

UPDATE users
SET email = 'employee.kim@skala-tech.local'
WHERE email = 'employee.kim@scala-tech.local'
  AND NOT EXISTS (
      SELECT 1 FROM (SELECT email FROM users) existing_users
      WHERE existing_users.email = 'employee.kim@skala-tech.local'
  );
