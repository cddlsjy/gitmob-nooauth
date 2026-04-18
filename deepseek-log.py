#!/usr/bin/env python3
"""
GitHub Actions 错误日志监控脚本（自动检测仓库版本）
功能：每 5 分钟自动拉取失败的工作流日志，保存到本地目录
使用：在任意 Git 仓库根目录下运行即可，无需手动配置仓库名。
依赖：需提前安装 GitHub CLI (gh) 并登录 (gh auth login)
"""

import os
import json
import subprocess
import time
import configparser
from pathlib import Path
from datetime import datetime

# ========== 配置区域（只需修改轮询间隔，仓库自动检测） ==========
POLL_INTERVAL = 300         # 轮询间隔（秒），5分钟 = 300秒
WORKFLOW_NAME = "build.yml"        # 工作流文件名（如 CI.yml），根据你的实际工作流名称修改
LOG_DIR = Path.home() / "ci_monitor_logs"  # 日志保存目录
# ================================================================

def get_repo_from_git():
    """从当前目录的 .git/config 读取 remote origin 的 URL，提取 owner/repo"""
    git_config_path = Path(".git/config")
    if not git_config_path.exists():
        print("❌ 当前目录不是 Git 仓库根目录（未找到 .git/config）")
        print("   请进入项目根目录后再运行脚本。")
        return None, None

    config = configparser.ConfigParser()
    config.read(git_config_path)
    if 'remote "origin"' not in config:
        print("❌ 未找到 remote 'origin'，请确保仓库已关联远程 GitHub 仓库。")
        return None, None

    url = config['remote "origin"']['url']
    # 支持 https://github.com/owner/repo.git 和 git@github.com:owner/repo.git
    if url.startswith("https://"):
        path = url.replace("https://github.com/", "").rstrip(".git")
    elif url.startswith("git@github.com:"):
        path = url.replace("git@github.com:", "").rstrip(".git")
    else:
        print(f"❌ 无法解析远程 URL: {url}")
        return None, None

    parts = path.split("/")
    if len(parts) >= 2:
        return parts[0], parts[1]
    else:
        print(f"❌ 无效的仓库路径: {path}")
        return None, None

def run_gh_command(cmd):
    """执行 gh CLI 命令并返回结果"""
    try:
        result = subprocess.run(cmd, shell=True, capture_output=True, text=True, check=True)
        return result.stdout.strip()
    except subprocess.CalledProcessError as e:
        print(f"❌ 命令执行失败: {cmd}\n错误: {e.stderr}")
        return None

def get_failed_runs(owner, repo):
    """获取最近失败的工作流运行记录"""
    cmd = (f"gh run list --repo {owner}/{repo} "
           f"--workflow {WORKFLOW_NAME} "
           f"--status failure --limit 10 --json databaseId,number,startedAt,url")
    output = run_gh_command(cmd)
    if not output:
        return []

    try:
        runs = json.loads(output)
        return runs
    except json.JSONDecodeError:
        print("❌ 解析 JSON 失败")
        return []

def download_run_log(owner, repo, run_id, run_number):
    """下载特定运行的日志到本地文件"""
    timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
    filename = f"{owner}_{repo}_fail_{timestamp}_run_{run_number}.log"
    filepath = LOG_DIR / filename

    cmd = f"gh run view {run_id} --repo {owner}/{repo} --log"
    log_content = run_gh_command(cmd)

    if log_content:
        with open(filepath, "w", encoding="utf-8") as f:
            f.write(f"Repo: {owner}/{repo}\n")
            f.write(f"Run ID: {run_id}\nRun Number: {run_number}\n")
            f.write(f"Download Time: {datetime.now().isoformat()}\n")
            f.write("=" * 50 + "\n")
            f.write(log_content)
        print(f"✅ 已保存日志: {filepath}")
        return filepath
    else:
        print(f"❌ 无法获取 Run {run_id} 的日志")
        return None

def main():
    # 自动检测仓库
    owner, repo = get_repo_from_git()
    if not owner or not repo:
        return

    print(f"📦 检测到仓库: {owner}/{repo}")
    print(f"📁 日志保存目录: {LOG_DIR}")
    print(f"⏱️  轮询间隔: {POLL_INTERVAL} 秒")
    print("🚀 监控已启动，按 Ctrl+C 停止")

    LOG_DIR.mkdir(parents=True, exist_ok=True)
    processed_runs = set()

    try:
        while True:
            print(f"\n[{datetime.now().strftime('%H:%M:%S')}] 检查失败运行...")
            failed_runs = get_failed_runs(owner, repo)
            if not failed_runs:
                print("✅ 暂无失败运行")
            else:
                for run in failed_runs:
                    run_id = run['databaseId']
                    run_number = run['number']
                    if run_id not in processed_runs:
                        print(f"⚠️  发现新失败运行: #{run_number} (ID: {run_id})")
                        download_run_log(owner, repo, run_id, run_number)
                        processed_runs.add(run_id)
            time.sleep(POLL_INTERVAL)
    except KeyboardInterrupt:
        print("\n👋 监控已停止")

if __name__ == "__main__":
    main()