FROM maven:3.9.9-eclipse-temurin-17 AS builder

ARG hwplib_git="https://github.com/neolord0/hwplib.git"
ARG hwpxlib_git="https://github.com/neolord0/hwpxlib.git"
ARG hwp2hwpx_git="https://github.com/neolord0/hwp2hwpx.git"

WORKDIR /app

# 새 폴더 생성 후 이동, Git 명령어 수행
RUN mkdir -p libs \
    && cd libs \
    && git clone ${hwplib_git} hwplib\
    && git clone ${hwpxlib_git} hwpxlib \
    && git clone ${hwp2hwpx_git} hwp2hwpx

RUN cd /app/libs/hwp2hwpx \
    && mvn clean install -DskipTests


COPY . .

# Maven 빌드
RUN mvn -pl server clean package -DskipTests


# -------------------
# 런타임 이미지
# -------------------
FROM eclipse-temurin:17-jdk-alpine
WORKDIR /app

# 빌드 결과물만 복사
COPY --from=builder /app/server/target/*.jar app.jar

ENTRYPOINT ["java", "-jar", "app.jar"]

