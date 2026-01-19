#!/usr/bin/env python3
"""
大規模なサンプルログファイル（約100MB）を生成するスクリプト
"""
import argparse
import random
from datetime import datetime, timedelta

# ログテンプレート
LOG_TEMPLATES = [
    ("{timestamp}\tINFO\tUserService\tUser login successful\tuser_id={user_id}\t{ip}",),
    ("{timestamp}\tDEBUG\tDatabasePool\tConnection acquired from pool\tpool_size={pool_size}\tthread-{thread}",),
    ("{timestamp}\tINFO\tOrderService\tOrder created\torder_id=ORD-{order_id}\tamount={amount}",),
    ("{timestamp}\tWARN\tPaymentGateway\tPayment retry attempt {retry}\ttransaction_id=TXN-{txn_id}\terror={error}",),
    ("{timestamp}\tERROR\tPaymentGateway\tPayment failed after retries\ttransaction_id=TXN-{txn_id}\terror={error}",),
    ("{timestamp}\tINFO\tEmailService\tEmail sent successfully\trecipient={email}\ttemplate={template}",),
    ("{timestamp}\tDEBUG\tCacheManager\tCache hit\tkey={cache_key}\tttl={ttl}",),
    ("{timestamp}\tINFO\tAuthService\tToken refreshed\tuser_id={user_id}\texpires_in={expires}",),
    ("{timestamp}\tWARN\tRateLimiter\tRate limit warning\tuser_id={user_id}\trequests={requests}\tlimit=100",),
    ("{timestamp}\tINFO\tProductService\tProduct viewed\tproduct_id=PROD-{product_id}\tcategory={category}",),
    ("{timestamp}\tERROR\tDatabasePool\tConnection timeout\tpool_size={pool_size}\twait_time_ms={wait_time}",),
    ("{timestamp}\tINFO\tUserService\tUser logout\tuser_id={user_id}\tsession_duration_sec={duration}",),
    ("{timestamp}\tDEBUG\tScheduledTask\tDaily report job started\tjob_id=DAILY-REPORT-{job_id}\t",),
    ("{timestamp}\tINFO\tReportService\tReport generated\treport_id=RPT-{report_id}\trecords={records}",),
    ("{timestamp}\tWARN\tDiskMonitor\tDisk usage high\tpath={path}\tusage_percent={usage}",),
    ("{timestamp}\tINFO\tBackupService\tBackup completed\tbackup_id=BKP-{backup_id}\tsize_mb={size}",),
    ("{timestamp}\tDEBUG\tHttpClient\tAPI request sent\tendpoint={endpoint}\tmethod={method}",),
    ("{timestamp}\tDEBUG\tHttpClient\tAPI response received\tstatus={status}\tduration_ms={duration_ms}",),
    ("{timestamp}\tERROR\tFileProcessor\tFile processing failed\tfile={filename}\terror={error}",),
    ("{timestamp}\tINFO\tNotificationService\tPush notification sent\tuser_id={user_id}\tmessage_type={msg_type}",),
    ("{timestamp}\tWARN\tSecurityService\tMultiple failed login attempts\tip_address={ip}\tattempts={attempts}",),
    ("{timestamp}\tINFO\tCartService\tItem added to cart\tuser_id={user_id}\tproduct_id=PROD-{product_id}",),
    ("{timestamp}\tDEBUG\tSessionManager\tSession created\tsession_id={session_id}\tuser_id={user_id}",),
    ("{timestamp}\tINFO\tSearchService\tSearch executed\tquery={query}\tresults={results}",),
    ("{timestamp}\tERROR\tImageProcessor\tImage resize failed\timage_id=IMG-{image_id}\terror={error}",),
    ("{timestamp}\tINFO\tOrderService\tOrder shipped\torder_id=ORD-{order_id}\ttracking_number={tracking}",),
    ("{timestamp}\tWARN\tApiGateway\tResponse time degraded\tendpoint={endpoint}\tavg_time_ms={avg_time}",),
    ("{timestamp}\tDEBUG\tMetricsCollector\tMetrics collected\tnamespace={namespace}\tmetric_count={count}",),
    ("{timestamp}\tFATAL\tCoreService\tCritical system error\terror_code={error_code}\tmessage={message}\t",),
    ("{timestamp}\tERROR\tThirdPartyService\tExternal API unavailable\tservice={service}\tretry_in_sec={retry_sec}",),
]

# データ生成用の値リスト
IP_ADDRESSES = [f"192.168.{random.randint(1, 255)}.{random.randint(1, 255)}" for _ in range(100)]
EMAILS = [f"user{i}@example.com" for i in range(1000)]
CATEGORIES = ["electronics", "books", "clothing", "home", "sports", "toys", "food"]
ERRORS = ["timeout", "connection_refused", "invalid_format", "out_of_memory", "disk_full", "not_found"]
QUERIES = ["laptop", "wireless headphones", "smartphone", "tablet", "camera", "printer", "keyboard"]
TEMPLATES = ["order_confirmation", "password_reset", "welcome", "newsletter", "promotion"]
ENDPOINTS = ["/api/v1/users", "/api/v1/orders", "/api/v1/products", "/api/v2/search", "/api/v1/payments"]
METHODS = ["GET", "POST", "PUT", "DELETE", "PATCH"]
PATHS = ["/var/log", "/var/data", "/home/app", "/tmp", "/opt/application"]
NAMESPACES = ["application", "database", "cache", "network", "security"]
SERVICES = ["payment_provider", "email_service", "sms_gateway", "analytics", "cdn"]

def generate_log_line(base_time, offset_seconds):
    """ログ行を生成"""
    timestamp = (base_time + timedelta(seconds=offset_seconds)).strftime("%Y-%m-%d %H:%M:%S")
    
    template = random.choice(LOG_TEMPLATES)[0]
    
    # テンプレートに値を埋め込む
    log_line = template.format(
        timestamp=timestamp,
        user_id=random.randint(10000, 99999),
        ip=random.choice(IP_ADDRESSES),
        pool_size=random.randint(5, 20),
        thread=random.randint(1, 10),
        order_id=random.randint(10000, 99999),
        amount=f"{random.uniform(100, 50000):.2f}",
        retry=random.randint(1, 5),
        txn_id=random.randint(10000, 99999),
        error=random.choice(ERRORS),
        email=random.choice(EMAILS),
        template=random.choice(TEMPLATES),
        cache_key=f"user:{random.randint(10000, 99999)}",
        ttl=random.choice([300, 600, 1800, 3600, 7200]),
        expires=random.choice([1800, 3600, 7200, 14400]),
        requests=random.randint(50, 99),
        product_id=random.randint(100, 999),
        category=random.choice(CATEGORIES),
        wait_time=random.randint(1000, 10000),
        duration=random.randint(60, 3600),
        job_id=f"{random.randint(1, 999):03d}",
        report_id=datetime.now().strftime("%Y%m%d") + f"{random.randint(1, 999):03d}",
        records=random.randint(100, 10000),
        path=random.choice(PATHS),
        usage=random.randint(70, 95),
        backup_id=datetime.now().strftime("%Y%m%d%H%M") + f"{random.randint(1, 99):02d}",
        size=random.randint(100, 5000),
        endpoint=random.choice(ENDPOINTS),
        method=random.choice(METHODS),
        status=random.choice([200, 201, 400, 404, 500, 503]),
        duration_ms=random.randint(50, 5000),
        filename=f"data_{datetime.now().strftime('%Y%m%d')}.csv",
        msg_type=random.choice(["promotion", "alert", "reminder", "update"]),
        attempts=random.randint(3, 10),
        session_id=f"SES-{''.join(random.choices('ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789', k=8))}",
        query=random.choice(QUERIES),
        results=random.randint(0, 100),
        image_id=random.randint(100, 999),
        tracking=f"TRK-{random.randint(1000000000, 9999999999)}",
        avg_time=random.randint(1000, 5000),
        namespace=random.choice(NAMESPACES),
        count=random.randint(10, 100),
        error_code=random.randint(5000, 5999),
        message="Critical error in core service",
        service=random.choice(SERVICES),
        retry_sec=random.randint(30, 300),
    )
    
    return log_line

def main():
    parser = argparse.ArgumentParser(
        description='大規模なサンプルログファイルを生成します',
        formatter_class=argparse.RawDescriptionHelpFormatter
    )
    parser.add_argument(
        '-s', '--size',
        type=int,
        default=100,
        help='生成するファイルサイズ (MB単位, デフォルト: 100)'
    )
    parser.add_argument(
        '-o', '--output',
        type=str,
        default='large_sample_log.tsv',
        help='出力ファイル名 (デフォルト: large_sample_log.tsv)'
    )
    
    args = parser.parse_args()
    
    target_size_mb = args.size
    target_size_bytes = target_size_mb * 1024 * 1024
    output_file = args.output
    
    print(f"生成中: {output_file} (目標: 約{target_size_mb}MB)")
    
    base_time = datetime(2026, 1, 17, 0, 0, 0)
    current_size = 0
    line_count = 0
    
    with open(output_file, 'w', encoding='utf-8') as f:
        while current_size < target_size_bytes:
            # 1秒ごとに1-3行のログを生成
            offset = line_count * random.uniform(0.1, 2.0)
            log_line = generate_log_line(base_time, offset)
            f.write(log_line + '\n')
            
            current_size += len(log_line.encode('utf-8')) + 1  # +1 for newline
            line_count += 1
            
            # 進捗表示
            if line_count % 50000 == 0:
                progress_mb = current_size / (1024 * 1024)
                print(f"  {line_count:,} 行生成済み ({progress_mb:.1f}MB / {target_size_mb}MB)")
    
    final_size_mb = current_size / (1024 * 1024)
    print(f"\n完了!")
    print(f"  ファイル: {output_file}")
    print(f"  行数: {line_count:,}")
    print(f"  サイズ: {final_size_mb:.2f}MB")

if __name__ == "__main__":
    main()
