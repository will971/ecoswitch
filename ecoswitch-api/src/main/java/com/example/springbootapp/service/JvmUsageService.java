package com.example.springbootapp.service;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.lang.management.RuntimeMXBean;
import java.lang.management.ThreadMXBean;

import org.springframework.stereotype.Service;

import com.sun.management.OperatingSystemMXBean;

@Service
public class JvmUsageService {

	public JvmUsageSnapshot snapshot() {
		MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();
		ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
		RuntimeMXBean runtimeMXBean = ManagementFactory.getRuntimeMXBean();
		OperatingSystemMXBean osBean = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();

		MemoryUsage heap = memoryMXBean.getHeapMemoryUsage();
		MemoryUsage nonHeap = memoryMXBean.getNonHeapMemoryUsage();

		double processCpuLoad = clampPercent(osBean.getProcessCpuLoad());
		double systemCpuLoad = clampPercent(osBean.getCpuLoad());

		return new JvmUsageSnapshot(
			toMb(heap.getUsed()),
			toMb(heap.getCommitted()),
			toMb(heap.getMax()),
			toMb(nonHeap.getUsed()),
			toMb(nonHeap.getCommitted()),
			threadMXBean.getThreadCount(),
			processCpuLoad,
			systemCpuLoad,
			runtimeMXBean.getUptime()
		);
	}

	private static long toMb(long bytes) {
		if (bytes < 0) {
			return -1;
		}
		return Math.round(bytes / 1024.0 / 1024.0);
	}

	private static double clampPercent(double value) {
		if (value < 0) {
			return -1;
		}
		return Math.round(value * 10000.0) / 100.0;
	}

	public record JvmUsageSnapshot(
		long heapUsedMb,
		long heapCommittedMb,
		long heapMaxMb,
		long nonHeapUsedMb,
		long nonHeapCommittedMb,
		int liveThreads,
		double processCpuPercent,
		double systemCpuPercent,
		long uptimeMs
	) {}
}
