package com.example.springbootapp.monitoring;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.stereotype.Component;

@Component
public class UsageMonitor {

	private final Map<String, UsageStat> stats = new ConcurrentHashMap<>();

	public void record(String component, String methodName, long elapsedNanos) {
		String key = component + "#" + methodName;
		UsageStat stat = stats.computeIfAbsent(key, ignored -> new UsageStat(component, methodName));
		stat.record(elapsedNanos);
	}

	public List<UsageSnapshot> getSnapshots() {
		List<UsageSnapshot> snapshots = new ArrayList<>();
		for (UsageStat stat : stats.values()) {
			snapshots.add(stat.snapshot());
		}
		snapshots.sort(Comparator.comparingLong(UsageSnapshot::callCount).reversed());
		return snapshots;
	}

	private static final class UsageStat {
		private final String component;
		private final String methodName;
		private final AtomicLong callCount = new AtomicLong();
		private final AtomicLong totalNanos = new AtomicLong();
		private final AtomicLong lastElapsedNanos = new AtomicLong();

		private UsageStat(String component, String methodName) {
			this.component = component;
			this.methodName = methodName;
		}

		private void record(long elapsedNanos) {
			callCount.incrementAndGet();
			totalNanos.addAndGet(elapsedNanos);
			lastElapsedNanos.set(elapsedNanos);
		}

		private UsageSnapshot snapshot() {
			long calls = callCount.get();
			double totalMs = nanosToMillis(totalNanos.get());
			double avgMs = calls == 0 ? 0.0 : totalMs / calls;
			double lastMs = nanosToMillis(lastElapsedNanos.get());
			return new UsageSnapshot(component, methodName, calls, round(totalMs), round(avgMs), round(lastMs));
		}

		private static double nanosToMillis(long nanos) {
			return nanos / 1_000_000.0;
		}

		private static double round(double value) {
			return Math.round(value * 100.0) / 100.0;
		}
	}

	public record UsageSnapshot(
		String component,
		String methodName,
		long callCount,
		double totalMs,
		double avgMs,
		double lastMs
	) {}
}
