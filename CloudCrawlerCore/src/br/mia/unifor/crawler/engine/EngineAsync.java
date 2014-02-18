package br.mia.unifor.crawler.engine;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import java.util.logging.Logger;

import br.mia.unifor.crawler.builder.ComputeProvider;
import br.mia.unifor.crawler.builder.factory.ComputeProviderFactory;
import br.mia.unifor.crawler.executer.Execution;
import br.mia.unifor.crawler.executer.artifact.Application;
import br.mia.unifor.crawler.executer.artifact.Benchmark;

import br.mia.unifor.crawler.executer.artifact.Scenario;

import br.mia.unifor.crawler.executer.artifact.VirtualMachine;
import br.mia.unifor.crawler.executer.artifact.WorkloadFunction;

import br.mia.unifor.crawler.parser.CrawlerParserYml;
import br.mia.unifor.crawler.parser.YamlLoader;

public class EngineAsync {

	protected static Logger logger = Logger.getLogger(EngineAsync.class
			.getName());

	public static Benchmark load(InputStream input, boolean validate) throws IOException, ValidationException {
		CrawlerParserYml parser = new CrawlerParserYml(input);
		Benchmark benchmark = YamlLoader.loadTest(parser.processLineByLine());
		
		if(validate){
			ValidationEngine.validate(benchmark);
		}
		
		return benchmark;
	}

	public static void stopInstances(List<VirtualMachine> instances)
			throws IOException {
		for (VirtualMachine instance : instances) {
			if (!"local".equals(instance.getType().getProvider().getName()) && instance.getProviderId() != null) {
				ComputeProvider provider = ComputeProviderFactory
						.getProvider(instance.getType().getProvider());

				logger.info("provider name=["+instance.getType().getProvider().getName()+"], instanceId=["+instance.getId() + "] stopping...");
				
				provider.stopInstance(instance);
				
				logger.info("provider name=["+instance.getType().getProvider().getName()+"], instanceId=["+instance.getId() + "] STOPPED");
				
			}
		}
	}

	public static void startInstances(Scenario scenario) throws IOException {
		startInstances(scenario.getApplication(), scenario.getApplication()
				.getVirtualMachines());
	}

	private static void startInstances(Application appliance,
			List<VirtualMachine> instances) throws IOException {

		for (VirtualMachine instance : instances) {
			if (!"local".equals(instance.getType().getProvider().getName())) {
				ComputeProvider provider = ComputeProviderFactory
						.getProvider(instance.getType().getProvider());

				provider.startInstance(instance, appliance);

				logger.info(instance.getId() + " pronta");
			}
		}
	}

	public static void stopLocalInstances(Scenario scenario, Benchmark benchmark)
			throws IOException {
		List<VirtualMachine> instances = scenario.getApplication()
				.getVirtualMachines();

		// removendo as instancias globais
		for (int i = 0; i < instances.size(); ++i) {
			VirtualMachine instance = instances.get(i);
			if (benchmark.getVirtualMachines().contains(instance)) {
				instances.remove(i);
			}
		}

		stopInstances(instances);
	}

	public static void startInstances(Benchmark benchmark)
			throws IOException {
		startInstances(null, benchmark.getVirtualMachines());
	}

	public static Boolean execTests(Scenario scenario, Benchmark benchmark,
			WorkloadFunction workloadFunction) throws Exception,
			InterruptedException {

		Boolean result = Boolean.TRUE;

		result &= Execution.execTests(scenario, benchmark, workloadFunction,
				scenario.getWorkload().getTargets());

		return result;
	}
}