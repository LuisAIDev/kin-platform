@echo off
cd /d C:\Users\Lenovo\proyecto-kin\kin-backend
mvnw flyway:migrate -Dflyway.url=jdbc:postgresql://ep-spring-dawn-ayhwgk7a-pooler.c-5.us-east-2.aws.neon.tech/neondb?sslmode=require -Dflyway.user=neondb_owner -Dflyway.password=ngg_iRk2at1BzJq5