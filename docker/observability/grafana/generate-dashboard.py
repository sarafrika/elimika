"""Generates dashboards/elimika-loadtest.json (the provisioned "Elimika load test" dashboard).

Edit the panel list below and run:  python3 docker/observability/grafana/generate-dashboard.py
Grafana re-reads the provisioned file within ~10s; no restart needed.
"""
import json, os
DS={"type":"prometheus","uid":"prometheus"}
C='container=~"app|postgres|meilisearch"'
APP='job="elimika-app"'
NOACT='uri!~"/actuator.*"'
rows=[
 ("Load (k6)",[
   ("k6 requests sent / s (by status)","reqps",[('sum(rate(k6_http_reqs_total{testid=~"$testid"}[$__rate_interval]))',"total"),('sum by (status) (rate(k6_http_reqs_total{testid=~"$testid"}[$__rate_interval]))',"{{status}}")]),
   ("k6 http_req_failed (share of requests)","percentunit",[('sum(rate(k6_http_reqs_total{testid=~"$testid",expected_response="false"}[$__rate_interval])) / sum(rate(k6_http_reqs_total{testid=~"$testid"}[$__rate_interval]))',"failed share")]),
   ("k6 latency p50 / p95 / p99 / max (worst request name)","s",[('max(k6_http_req_duration_p50{testid=~"$testid"})',"p50"),('max(k6_http_req_duration_p95{testid=~"$testid"})',"p95"),('max(k6_http_req_duration_p99{testid=~"$testid"})',"p99"),('max(k6_http_req_duration_max{testid=~"$testid"})',"max")]),
   ("k6 p95 by request name","s",[('max by (name) (k6_http_req_duration_p95{testid=~"$testid"})',"{{name}}")]),
   ("k6 p99 by request name","s",[('max by (name) (k6_http_req_duration_p99{testid=~"$testid"})',"{{name}}")]),
   ("k6 virtual users","short",[('sum(k6_vus{testid=~"$testid"})',"vus"),('sum(k6_vus_max{testid=~"$testid"})',"vus max")]),
 ]),
 ("App (server side)",[
   ("Server p95 by uri","s",[(f'histogram_quantile(0.95, sum by (le, uri) (rate(http_server_requests_seconds_bucket{{{APP},{NOACT}}}[$__rate_interval])))',"{{uri}}")]),
   ("Server p99 by uri","s",[(f'histogram_quantile(0.99, sum by (le, uri) (rate(http_server_requests_seconds_bucket{{{APP},{NOACT}}}[$__rate_interval])))',"{{uri}}")]),
   ("Server throughput by status","reqps",[(f'sum by (status) (rate(http_server_requests_seconds_count{{{APP},{NOACT}}}[$__rate_interval]))',"{{status}}")]),
   ("Server p50 / p95 / p99 (all uris)","s",[(f'histogram_quantile({q}, sum by (le) (rate(http_server_requests_seconds_bucket{{{APP},{NOACT}}}[$__rate_interval])))',f"p{int(float(q)*100)}") for q in ("0.5","0.95","0.99")]),
   ("Tomcat threads","short",[(f"tomcat_threads_busy_threads{{{APP}}}","busy"),(f"tomcat_threads_current_threads{{{APP}}}","current"),(f"tomcat_threads_config_max_threads{{{APP}}}","max")]),
   ("Process CPU","percentunit",[(f"process_cpu_usage{{{APP}}}","process"),(f"system_cpu_usage{{{APP}}}","system")]),
 ]),
 ("DB pool (Hikari)",[
   ("Connections active / idle / pending","short",[(f"sum(hikaricp_connections_active{{{APP}}})","active"),(f"sum(hikaricp_connections_idle{{{APP}}})","idle"),(f"sum(hikaricp_connections_pending{{{APP}}})","pending (waiting threads)"),(f"sum(hikaricp_connections_max{{{APP}}})","max")]),
   ("Connection timeouts / s","short",[(f"sum(rate(hikaricp_connections_timeout_total{{{APP}}}[$__rate_interval]))","timeouts/s"),(f"sum(increase(hikaricp_connections_timeout_total{{{APP}}}[$__range]))","total in range")]),
   ("Connection acquire time","s",[(f"sum(rate(hikaricp_connections_acquire_seconds_sum{{{APP}}}[$__rate_interval])) / sum(rate(hikaricp_connections_acquire_seconds_count{{{APP}}}[$__rate_interval]))","mean"),(f"max(hikaricp_connections_acquire_seconds_max{{{APP}}})","max")]),
   ("Connection usage (held) time","s",[(f"sum(rate(hikaricp_connections_usage_seconds_sum{{{APP}}}[$__rate_interval])) / sum(rate(hikaricp_connections_usage_seconds_count{{{APP}}}[$__rate_interval]))","mean"),(f"max(hikaricp_connections_usage_seconds_max{{{APP}}})","max")]),
 ]),
 ("JVM",[
   ("Heap used / committed / max","bytes",[(f'sum(jvm_memory_used_bytes{{{APP},area="heap"}})',"used"),(f'sum(jvm_memory_committed_bytes{{{APP},area="heap"}})',"committed"),(f'sum(jvm_memory_max_bytes{{{APP},area="heap"}})',"max")]),
   ("Non-heap used","bytes",[(f'sum by (id) (jvm_memory_used_bytes{{{APP},area="nonheap"}})',"{{id}}")]),
   ("GC pause","s",[(f"sum by (gc) (rate(jvm_gc_pause_seconds_sum{{{APP}}}[$__rate_interval]))","{{gc}} pause s/s"),(f"max by (gc) (jvm_gc_pause_seconds_max{{{APP}}})","{{gc}} max")]),
   ("Live threads","short",[(f"jvm_threads_live_threads{{{APP}}}","live"),(f"jvm_threads_peak_threads{{{APP}}}","peak")]),
 ]),
 ("Container (cAdvisor)",[
   ("CPU % (100 = one core)","percent",[(f"sum by (container) (rate(container_cpu_usage_seconds_total{{{C}}}[$__rate_interval])) * 100","{{container}}")]),
   ("Memory working set","bytes",[(f"sum by (container) (container_memory_working_set_bytes{{{C}}})","{{container}}"),(f'max(container_spec_memory_limit_bytes{{container="app"}})',"app limit")]),
   ("CPU throttling (app)","percentunit",[('sum(rate(container_cpu_cfs_throttled_periods_total{container="app"}[$__rate_interval])) / sum(rate(container_cpu_cfs_periods_total{container="app"}[$__rate_interval]))',"throttled periods")]),
 ]),
 ("Postgres",[
   ("pg_stat_activity by state","short",[('sum by (state) (pg_stat_activity_count{datname="elimika"})',"{{state}}")]),
   ("Transactions / s","short",[('sum(rate(pg_stat_database_xact_commit{datname="elimika"}[$__rate_interval]))',"commit"),('sum(rate(pg_stat_database_xact_rollback{datname="elimika"}[$__rate_interval]))',"rollback")]),
   ("Locks by mode","short",[('sum by (mode) (pg_locks_count{datname="elimika"})',"{{mode}}")]),
   ("Cache hit ratio","percentunit",[('sum(rate(pg_stat_database_blks_hit{datname="elimika"}[$__rate_interval])) / (sum(rate(pg_stat_database_blks_hit{datname="elimika"}[$__rate_interval])) + sum(rate(pg_stat_database_blks_read{datname="elimika"}[$__rate_interval])))',"hit ratio")]),
 ]),
]
panels=[];pid=1;y=0
for rtitle,ps in rows:
    panels.append({"type":"row","title":rtitle,"id":pid,"collapsed":False,"gridPos":{"h":1,"w":24,"x":0,"y":y},"panels":[]})
    pid+=1;y+=1
    w=24//min(len(ps),3) if len(ps)!=4 else 12
    x=0
    for title,unit,targets in ps:
        if x+w>24: x=0;y+=8
        panels.append({"type":"timeseries","title":title,"id":pid,"datasource":DS,
          "gridPos":{"h":8,"w":w,"x":x,"y":y},
          "fieldConfig":{"defaults":{"unit":unit,"custom":{"lineWidth":1,"fillOpacity":8,"showPoints":"never","spanNulls":True}},"overrides":[]},
          "options":{"legend":{"displayMode":"list","placement":"bottom","showLegend":True},"tooltip":{"mode":"multi","sort":"desc"}},
          "targets":[{"refId":chr(65+i),"datasource":DS,"expr":e,"legendFormat":l,"range":True} for i,(e,l) in enumerate(targets)]})
        pid+=1;x+=w
    y+=8
dash={"uid":"elimika-loadtest","title":"Elimika load test","tags":["elimika","loadtest"],"timezone":"utc",
 "schemaVersion":41,"version":1,"editable":True,"refresh":"5s","time":{"from":"now-30m","to":"now"},
 "graphTooltip":1,"panels":panels,"templating":{"list":[{"name":"testid","label":"k6 testid","type":"query","datasource":DS,"query":{"query":"label_values(k6_http_reqs_total, testid)","refId":"q"},"definition":"label_values(k6_http_reqs_total, testid)","refresh":2,"includeAll":True,"allValue":".*","multi":True,"current":{"selected":True,"text":["All"],"value":["$__all"]}}]},"annotations":{"list":[]}}
out=os.path.join(os.path.dirname(os.path.abspath(__file__)),"dashboards","elimika-loadtest.json")
json.dump(dash,open(out,"w"),indent=2)
# panel index (ids are what /d-solo/ URLs and screenshot.sh use)
for p in panels: print(p["id"],p["type"],p["title"])
