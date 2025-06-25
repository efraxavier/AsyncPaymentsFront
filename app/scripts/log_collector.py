import subprocess
import time
import requests

LOG_FILE = "asyncpayments_testes.log"
BACKEND_URL = "http://localhost:8080/api/logs" 

# Inicia o logcat em background
logcat_proc = subprocess.Popen([
    "adb", "logcat",
    "-s", "TransactionManager:V", "TransactionActivity:V", "OfflineTransactionQueue:V",
    "ShowNotification:V", "AddFundsActivity:V", "SMSReceiver:V", "TokenUtils:V"
], stdout=open(LOG_FILE, "w"), stderr=subprocess.STDOUT)

try:
    while True:
        time.sleep(60)  # a cada 60 segundos
        with open(LOG_FILE, "r") as f:
            logs = f.read()
        # Envia logs para o backend
        response = requests.post(BACKEND_URL, data={"logs": logs})
        print("Logs enviados, status:", response.status_code)
        # Limpa o arquivo após envio (opcional)
        open(LOG_FILE, "w").close()
except KeyboardInterrupt:
    logcat_proc.terminate()