/**
 * Copyright (c) 2016, All partners of the iTesla project (http://www.itesla-project.eu/consortium)
 * Copyright (c) 2016, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package eu.itesla_project.online.tools;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.joda.JodaModule;
import com.google.auto.service.AutoService;
import eu.itesla_project.commons.tools.Command;
import eu.itesla_project.commons.tools.Tool;
import eu.itesla_project.computation.local.LocalComputationManager;
import eu.itesla_project.cases.CaseRepository;
import eu.itesla_project.cases.CaseType;
import eu.itesla_project.modules.online.OnlineConfig;
import eu.itesla_project.modules.online.OnlineDb;
import eu.itesla_project.modules.online.OnlineProcess;
import eu.itesla_project.modules.online.OnlineWorkflowParameters;
import eu.itesla_project.online.LocalOnlineApplicationMBean;
import eu.itesla_project.online.OnlineWorkflowStartParameters;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionGroup;
import org.apache.commons.cli.Options;
import org.apache.commons.io.IOUtils;
import org.joda.time.DateTime;
import org.joda.time.Interval;
import javax.management.MBeanServerConnection;
import javax.management.MBeanServerInvocationHandler;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;


/**
 *
 * @author Giovanni Ferrari <giovanni.ferrari@techrain.it>
 */
@AutoService(Tool.class)
public class OnlineProcessTool implements Tool {

	private static Command COMMAND = new Command() {

		@Override
		public String getName() {
			return "start-online-process";
		}

		@Override
		public String getTheme() {
			return Themes.ONLINE_PROCESS;
		}

		@Override
		public String getDescription() {
			return "start an online process";
		}

		@Override
		public Options getOptions() {
			Options options = new Options();

			options.addOption(Option.builder().longOpt("config-file").desc("").hasArg().argName("CONFIG-FILE").build());		

			options.addOption(Option.builder().longOpt("case-type").desc("case type {SN,FO}").hasArg()
					.argName("CASE_TYPE").build());

			options.addOption(
					Option.builder().longOpt("name").desc("process name").hasArg().argName("NAME").build());
			options.addOption(Option.builder().longOpt("owner").desc("process owner").hasArg()
					.argName("OWNER").build());
			options.addOption(Option.builder().longOpt("basecases-interval")
					.desc("interval for basecases to be considered").hasArg()
					.argName("BASECASES_INTERVAL").build());
			options.addOption(
					Option.builder().longOpt("states").desc("States number").hasArg().argName("STATES").build());
			options.addOption(Option.builder().longOpt("date").desc("Process date").hasArg().argName("DATE").build());
			options.addOption(
					Option.builder().longOpt("creation-date").desc("Process creation date").hasArg().argName("CREATION_DATE").build());
			
			return options;
		}

		@Override
		public String getUsageFooter() {
			return null;
		}

	};

	@Override
	public Command getCommand() {
		return COMMAND;
	}

	@Override
	public void run(CommandLine line) throws Exception {

		OnlineWorkflowStartParameters startconfig = OnlineWorkflowStartParameters.loadDefault();

		OnlineWorkflowParameters params = OnlineWorkflowParameters.loadDefault();
		OnlineProcessParameters proc_params=null;
		if (line.hasOption("config-file"))
		{
			proc_params=readParamsFile(line.getOptionValue("config-file"));
			
		}
		else
			proc_params=new OnlineProcessParameters();
		
		readParams(line,proc_params);
		
		
		if(proc_params==null)
			return;
		
		if(proc_params.getDate()==null)
			proc_params.setDate(new DateTime());
		if(proc_params.getCreationDate()==null)
			proc_params.setCreationDate(new DateTime());
		
		
		System.out.println("OnlineProcess config: "+proc_params);

		if(proc_params.getCaseType()!=null)
			params.setCaseType(CaseType.valueOf(proc_params.getCaseType()));
		
		if(params.getCaseType()==null)
		{
			System.out.println("Error: Missing required param 'case-type'");
			return;
		}		
		
		
		if(proc_params.getStates()!=null)
			params.setStates(proc_params.getStates());

		OnlineConfig oConfig = OnlineConfig.load();
		CaseRepository caseRepo = oConfig.getCaseRepositoryFactoryClass().newInstance()
				.create(new LocalComputationManager());

		DateTime[] basecases = null;

		Set<DateTime> baseCasesSet = null;
		
		if (proc_params.getBasecasesInterval()!=null) {
			Interval basecasesInterval = Interval.parse(proc_params.getBasecasesInterval());
			baseCasesSet = caseRepo.dataAvailable(params.getCaseType(), params.getCountries(), basecasesInterval);
			if (baseCasesSet.isEmpty()) {
				System.out.println("No Base cases available for case-type " + params.getCaseType() + " and interval "
						+ proc_params.getBasecasesInterval());
				return;
			}
			System.out.println("Base cases available for interval " + basecasesInterval.toString());
			baseCasesSet.forEach(x -> {
				System.out.println(" " + x);
			});
			basecases = new DateTime[baseCasesSet.size()];
			basecases = baseCasesSet.toArray(basecases);
		} else {
			if (params.getCaseType().equals(CaseType.FO)) {
				DateTime endOfDay = new DateTime(proc_params.getCreationDate().getYear(), proc_params.getCreationDate().getMonthOfYear(),
						proc_params.getCreationDate().getDayOfMonth(), 23, 59, 59, 999);
				DateTime endOfTomorrow = endOfDay.plusDays(1);
				Interval basecasesInterval = new Interval(proc_params.getCreationDate(), endOfTomorrow);
				baseCasesSet = caseRepo.dataAvailable(params.getCaseType(), params.getCountries(), basecasesInterval);
				basecases = new DateTime[baseCasesSet.size()];
				basecases = baseCasesSet.toArray(basecases);
				if (baseCasesSet.isEmpty()) {
					System.out.println("No Base cases available for  case-type " + params.getCaseType()
							+ " and interval " + basecasesInterval);
					return;
				}
				System.out.println("Base cases available for interval " + basecasesInterval.toString());
				baseCasesSet.forEach(x -> {
					System.out.println(" " + x);
				});
			} else if (params.getCaseType().equals(CaseType.SN)) {
				DateTime startOfDay = new DateTime(proc_params.getCreationDate().getYear(), proc_params.getCreationDate().getMonthOfYear(),
						proc_params.getCreationDate().getDayOfMonth(), 0, 0, 0, 0);

				Interval basecasesInterval = new Interval(startOfDay, proc_params.getCreationDate());
				baseCasesSet = caseRepo.dataAvailable(params.getCaseType(), params.getCountries(), basecasesInterval);
				if (baseCasesSet.isEmpty()) {
					System.out.println("No Base cases available for case-type " + params.getCaseType()
							+ " and interval " + basecasesInterval);
					return;
				}
				DateTime max = Collections.max(baseCasesSet);
				basecases = new DateTime[1];
				basecases[0] = max;
				System.out.println("SN - Latest Day Base case is " + max + " for interval " + basecasesInterval);
				// CHeck if already processed

				boolean processed=false;
				OnlineConfig config = OnlineConfig.load();
				OnlineDb onlinedb = config.getOnlineDbFactoryClass().newInstance().create();
				try {
					List<OnlineProcess> plist = onlinedb.listProcesses();
					for(OnlineProcess p : plist)
					{
						if(p.getCaseType().equals(CaseType.SN.toString())	&& p.getWorkflowsMap().containsKey(max.toString()))
						{
							processed=true;
							break;
						}
					}
					
				} finally {
					try {
						onlinedb.close();
					} catch (Throwable t) {
					}
				}
				if(processed)
				{
					System.out.println("SN - Base case already processed: " + max+" - Exiting");
					return;
				}


			}

		}



		String urlString = "service:jmx:rmi:///jndi/rmi://" + startconfig.getJmxHost() + ":" + startconfig.getJmxPort()
				+ "/jmxrmi";

		JMXServiceURL serviceURL = new JMXServiceURL(urlString);
		Map<String, String> jmxEnv = new HashMap<>();
		JMXConnector connector = JMXConnectorFactory.connect(serviceURL, jmxEnv);
		MBeanServerConnection mbsc = connector.getMBeanServerConnection();

		ObjectName objname = new ObjectName(LocalOnlineApplicationMBean.BEAN_NAME);

		LocalOnlineApplicationMBean application = MBeanServerInvocationHandler.newProxyInstance(mbsc, objname,
				LocalOnlineApplicationMBean.class, false);

		application.startProcess(proc_params.getName(), proc_params.getOwner(), proc_params.getDate(), proc_params.getCreationDate(), startconfig, params, basecases);
	}
	
	private void readParams(CommandLine line, OnlineProcessParameters pp) {
		if(line.hasOption("name"))
			pp.setName(line.getOptionValue("name"));
		
		if(line.hasOption("owner"))
			pp.setOwner(line.getOptionValue("owner"));
		
		if(line.hasOption("basecases-interval"))
			pp.setBasecasesInterval( line.getOptionValue("basecases-interval"));
		
		if(line.hasOption("states"))
			pp.setStates(new Integer(line.getOptionValue("states")));
				
		if (line.hasOption("case-type"))
			pp.setCaseType(line.getOptionValue("case-type"));
				
		if (line.hasOption("date")) 
			pp.setDate(DateTime.parse(line.getOptionValue("date")));		

		if (line.hasOption("creation-date")) 
			pp.setCreationDate(DateTime.parse(line.getOptionValue("creation-date")));			

	}

	private OnlineProcessParameters readParamsFile(String filename) throws JsonParseException, JsonMappingException, IOException {
		Path procFile=Paths.get(filename);
       	if(Files.exists(procFile))
       	{
       		InputStream is = new FileInputStream(procFile.toFile());
            String json = IOUtils.toString(is);
			ObjectMapper objectMapper = new ObjectMapper();
			objectMapper.registerModule(new JodaModule());
			objectMapper.configure(com.fasterxml.jackson.databind.SerializationFeature.
			    WRITE_DATES_AS_TIMESTAMPS , false);
			return objectMapper.readValue(json,OnlineProcessParameters.class);
		}
       	else
       		System.out.println("File not found: "+filename);
		return null;
	}


	
	

}
