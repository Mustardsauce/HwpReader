# 📝 HwpReader REST API Server

한글 문서(`.hwp`, `.hwpx`)에서 **텍스트와 표 구조를 추출**하는 Java 기반 REST API 서버입니다.  
Docker 기반으로 쉽게 배포할 수 있으며, 표의 행(`ROW`)·셀(`CELL`) 단위로 구조화된 데이터를 반환합니다.

---

## 🚀 프로젝트 개요

- **기술 스택:** Java 17 + Spring Boot + Maven + Docker  
- **지원 포맷:** `.hwp`, `.hwpx`  
- **주요 기능:**  
  - 한글 문서 내 텍스트 추출  
  - 표(`TABLE`)의 행(`ROW`), 셀(`CELL`) 구조 표시  
  - HWP 파일을 자동으로 HWPX로 변환 후 통합 처리  
- **엔드포인트:**  
  - `POST /api/reader/extract` : 파일 업로드 후 내용 추출

---

## 서버 구동 방법

### Setup
```bash
git clone https://github.com/Mustardsauce/HwpReader.git
cd HwpReader
docker-compose up -d
```

### docker image pull
```bash
docker pull mustards94/hwp-reader:latest
```

### docker-compose.yaml
```yaml
services:
  hwp-rest-server:    
    build:
      context: .
      dockerfile: Dockerfile
    container_name: HwpReader
    image: mustards94/hwp-reader:latest
    ports:
      - "${SERVER_PORT}:${SERVER_PORT}"
    environment:
      SERVER_PORT: ${SERVER_PORT}
      SPRING_SERVLET_MULTIPART_MAX_FILE_SIZE: ${MAX_FILE_SIZE}
      SPRING_SERVLET_MULTIPART_MAX_REQUEST_SIZE: ${MAX_REQUEST_SIZE}
    restart: unless-stopped
```

### 서버 요청 예시
```bash
curl -X POST http://host.docker.internal:${HwpReader 포트}/api/reader/extract -F "file=@/path/to/hwp or hwpx"
```



---

## 📚 사용된 오픈소스 라이브러리

| 라이브러리 | 설명 | GitHub 링크 |
|-------------|------|--------------|
| **hwplib** | 한글(HWP) 바이너리 파일 파서 | [neolord0/hwplib](https://github.com/neolord0/hwplib.git) |
| **hwpxlib** | 한글(HWPX) XML 기반 문서 파서 | [neolord0/hwpxlib](https://github.com/neolord0/hwpxlib.git) |
| **hwp2hwpx** | `.hwp` → `.hwpx` 변환 유틸리티 | [neolord0/hwp2hwpx](https://github.com/neolord0/hwp2hwpx.git) |

> 세 라이브러리는 모두 neolord0에서 관리하는 오픈소스로, 본 서버는 이를 조합하여 `.hwp`/`.hwpx` 통합 처리 기능을 제공합니다.
