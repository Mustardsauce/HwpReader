#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
HWP Reader API 테스트 스크립트
"""

import requests
import json
from pathlib import Path
import os
# ============================================
# 설정 (여기를 수정하세요)
# ============================================
SERVER_URL = "http://localhost:8500"

TEST_FILE = os.path.join(os.path.dirname(__file__), "test_data", "changing-image.hwp")  # 테스트할 hwp 또는 hwpx 파일 경로
OUTPUT_FILE = os.path.join(os.path.dirname(__file__), "output.hwpx")  # 변환된 hwpx 파일 저장 경로

# ============================================
# 테스트 함수들
# ============================================

def test_extract_api(file_path):
    """텍스트 추출 API 테스트"""
    print("=" * 60)
    print("📄 텍스트 추출 API 테스트")
    print("=" * 60)
    
    url = f"{SERVER_URL}/api/reader/extract"
    
    try:
        with open(file_path, 'rb') as f:
            files = {'file': (Path(file_path).name, f, 'application/octet-stream')}
            response = requests.post(url, files=files)
        
        print(f"✅ 상태 코드: {response.status_code}")
        
        if response.status_code == 200:
            data = response.json()
            print(f"✅ 응답 상태: {data.get('status')}")
            print(f"\n📝 추출된 텍스트:\n")
            print("-" * 60)
            document = data.get('document', '')
            # 너무 길면 일부만 출력
            if len(document) > 500:
                print(document[:500])
                print(f"\n... (총 {len(document)}자)")
            else:
                print(document)
            print("-" * 60)
            return True
        else:
            print(f"❌ 에러: {response.text}")
            return False
            
    except FileNotFoundError:
        print(f"❌ 파일을 찾을 수 없습니다: {file_path}")
        return False
    except requests.exceptions.ConnectionError:
        print(f"❌ 서버에 연결할 수 없습니다: {SERVER_URL}")
        return False
    except Exception as e:
        print(f"❌ 예외 발생: {e}")
        return False


def test_convert_api(file_path, output_path):
    """hwpx 변환 API 테스트"""
    print("\n" + "=" * 60)
    print("🔄 HWPX 변환 API 테스트")
    print("=" * 60)
    
    url = f"{SERVER_URL}/api/reader/content"
    
    try:
        with open(file_path, 'rb') as f:
            files = {'file': (Path(file_path).name, f, 'application/octet-stream')}
            response = requests.post(url, files=files)
        
        print(f"✅ 상태 코드: {response.status_code}")
        
        if response.status_code == 200:
            # 바이너리 데이터 저장
            with open(output_path, 'wb') as f:
                f.write(response.content)
            
            file_size = len(response.content)
            print(f"✅ 변환 성공!")
            print(f"✅ 파일 크기: {file_size:,} bytes ({file_size/1024:.2f} KB)")
            print(f"✅ 저장 경로: {output_path}")
            
            # 파일이 실제로 생성되었는지 확인
            if Path(output_path).exists():
                print(f"✅ 파일 생성 확인됨")
                return True
            else:
                print(f"❌ 파일 생성 실패")
                return False
        else:
            print(f"❌ 에러 (상태 코드: {response.status_code})")
            return False
            
    except FileNotFoundError:
        print(f"❌ 파일을 찾을 수 없습니다: {file_path}")
        return False
    except requests.exceptions.ConnectionError:
        print(f"❌ 서버에 연결할 수 없습니다: {SERVER_URL}")
        return False
    except Exception as e:
        print(f"❌ 예외 발생: {e}")
        return False


def test_health_check():
    """서버 상태 확인"""
    print("=" * 60)
    print("🏥 서버 상태 확인")
    print("=" * 60)
    
    try:
        # Spring Boot Actuator가 없으므로 extract API로 간단히 확인
        response = requests.get(f"{SERVER_URL}/api/reader/extract", timeout=3)
        # GET 요청은 405 Method Not Allowed를 반환하지만 서버가 살아있다는 의미
        if response.status_code in [200, 405]:
            print(f"✅ 서버 응답 확인됨")
            return True
        else:
            print(f"⚠️  서버 응답: {response.status_code}")
            return True
    except requests.exceptions.ConnectionError:
        print(f"❌ 서버에 연결할 수 없습니다: {SERVER_URL}")
        return False
    except Exception as e:
        print(f"❌ 예외 발생: {e}")
        return False


# ============================================
# 메인 실행
# ============================================

def main():
    print("\n")
    print("🚀 HWP Reader API 테스트 시작")
    print(f"📍 서버 주소: {SERVER_URL}")
    print(f"📁 테스트 파일: {TEST_FILE}")
    print("\n")
    
    # 1. 서버 상태 확인
    if not test_health_check():
        print("\n❌ 서버가 실행 중이 아닙니다. 테스트를 중단합니다.")
        return
    
    # 2. 텍스트 추출 API 테스트
    extract_result = test_extract_api(TEST_FILE)
    
    # 3. 변환 API 테스트
    convert_result = test_convert_api(TEST_FILE, OUTPUT_FILE)
    
    # 결과 요약
    print("\n" + "=" * 60)
    print("📊 테스트 결과 요약")
    print("=" * 60)
    print(f"텍스트 추출 API: {'✅ 성공' if extract_result else '❌ 실패'}")
    print(f"HWPX 변환 API:   {'✅ 성공' if convert_result else '❌ 실패'}")
    print("=" * 60)
    
    if extract_result and convert_result:
        print("\n🎉 모든 테스트 통과!")
    else:
        print("\n⚠️  일부 테스트 실패")
    
    print("\n")


if __name__ == "__main__":
    main()

