import os
import uuid
import shutil
import tempfile
import datetime
from typing import Optional

import requests
from fastapi import FastAPI, File, UploadFile, Form, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel
from ultralytics import YOLO

# ─────────────────────────────────────────────────────────────
# 환경변수
MODEL_PATH = os.getenv("MODEL_PATH", "/app/weights/best.pt")
CONF_THRESH = float(os.getenv("CONF_THRESH", "0.20"))
# ─────────────────────────────────────────────────────────────

app = FastAPI(title="YOLOv8 Python API", version="1.0.0")

# CORS (필요시 출처 제한)
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"], allow_credentials=True,
    allow_methods=["*"], allow_headers=["*"],
)

# YOLO 모델 로딩 (앱 시작 시 1회)
try:
    model = YOLO(MODEL_PATH)
except Exception as e:
    raise RuntimeError(f"YOLO model load failed: {e}") from e


# 응답 모델
class PredictResponse(BaseModel):
    timestamp: str
    source: str
    beachName: Optional[str] = None
    personCount: int
    congestion: Optional[str] = None  # 원하면 파이썬에서도 등급 산출
    conf: float
    model: str


def compute_congestion(count: int, t1: int, t2: int, t3: int) -> str:
    """
    간단한 등급 함수 (옵션)
    - count <= t1  : 여유
    - t1 < count <= t2 : 보통
    - t2 < count <= t3 : 혼잡
    - t3 < count : 매우 혼잡
    """
    if count <= t1:
        return "여유"
    elif count <= t2:
        return "보통"
    elif count <= t3:
        return "혼잡"
    return "매우 혼잡"


@app.get("/health")
def health():
    return {"status": "ok", "model": os.path.basename(MODEL_PATH), "conf": CONF_THRESH}


@app.post("/predict", response_model=PredictResponse)
async def predict_file(
    file: UploadFile = File(None, description="이미지 파일 업로드 (multipart/form-data)"),
    image_url: Optional[str] = Form(None, description="이미지 URL (택1)"),
    beachName: Optional[str] = Form(None),
    # 파이썬 쪽에서 등급도 함께 받고 싶으면 임계치 전달(옵션)
    t1: Optional[int] = Form(None),
    t2: Optional[int] = Form(None),
    t3: Optional[int] = Form(None),
    conf: Optional[float] = Form(None, description="추론 confidence(기본=ENV CONF_THRESH)"),
):
    """
    - 파일 업로드 또는 URL 중 하나만 넘기면 됩니다.
    - classes=[0] 로 사람만 감지 → 카운트는 len(boxes)
    - t1/t2/t3 를 넘기면 파이썬에서 congestion도 함께 산출해서 반환합니다.
      (안 넘기면 personCount만 반환하고, 등급은 스프링에서 계산)
    """
    if file is None and not image_url:
        raise HTTPException(status_code=400, detail="file 또는 image_url 중 하나는 필수입니다.")

    # 임시 파일 생성
    tmp_dir = tempfile.mkdtemp(dir="/app/tmp")
    tmp_path = os.path.join(tmp_dir, f"{uuid.uuid4().hex}.jpg")

    try:
        # 1) 파일 업로드
        if file:
            with open(tmp_path, "wb") as buffer:
                shutil.copyfileobj(file.file, buffer)
            source_desc = f"upload:{file.filename}"

        # 2) URL 다운로드
        else:
            try:
                r = requests.get(image_url, timeout=10)
                r.raise_for_status()
            except Exception as e:
                raise HTTPException(status_code=400, detail=f"이미지 URL 다운로드 실패: {e}")
            with open(tmp_path, "wb") as f:
                f.write(r.content)
            source_desc = f"url:{image_url}"

        # 추론
        conf_th = float(conf) if conf is not None else CONF_THRESH
        # 사람 클래스만 대상으로 추론(classes=[0])
        res = model.predict(source=tmp_path, conf=conf_th, classes=[0], save=False)
        boxes = res[0].boxes

        # 사람 수 = 감지된 박스 개수
        person_count = len(boxes)

        # 파이썬에서도 등급 계산을 원할 경우에만 계산
        congestion = None
        if t1 is not None and t2 is not None and t3 is not None:
            congestion = compute_congestion(person_count, t1, t2, t3)

        return PredictResponse(
            timestamp=datetime.datetime.now().isoformat(),
            source=source_desc,
            beachName=beachName,
            personCount=person_count,
            congestion=congestion,
            conf=conf_th,
            model=os.path.basename(MODEL_PATH)
        )

    finally:
        # 임시 파일/폴더 정리
        try:
            if os.path.exists(tmp_path):
                os.remove(tmp_path)
            shutil.rmtree(tmp_dir, ignore_errors=True)
        except Exception:
            pass
