/*******************************************************************************
 * Copyright 2018 RaspInLoop
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.  You may obtain a copy
 * of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 ******************************************************************************/
package org.raspinloop.agent;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import org.raspinloop.hwemulation.GpioProvider;
import org.raspinloop.hwemulation.HwEmulationFactory;
import org.raspinloop.hwemulation.HwEmulationFactoryFromJson;
import org.raspinloop.timeemulation.SimulatedTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class Handler {

	public static Handler build(String jsonConfig) throws Exception {
		return new Handler(jsonConfig);
	}

	final static Logger logger = LoggerFactory.getLogger(Handler.class);

	final Executor ex = Executors.newCachedThreadPool();

	private CSHandler fmiHandler;
	
	private boolean readyForMain = false;
	
	public boolean isReadyForMain() {
		return readyForMain;
	}

	private void setReadyForMain() {
		this.readyForMain = true;
	}

	private Handler(String jsonConfig) throws Exception {

		fmiHandler = CSHandler.getInstance();
		

		HwEmulationFactory factory = new HwEmulationFactoryFromJson();

		if (factory.create(jsonConfig) == null){
			logger.error("Cannot create instance for json["+jsonConfig+"]");
			throw new Exception("Cannot create hardware for json");
		}
		
		fmiHandler.registerHardware(factory);

//		final SimulatedTimeExecutorServiceFactory simulatedTimeExecutorFactory = new SimulatedTimeExecutorServiceFactory();
		fmiHandler.addExperimentListener(new ExperimentListener() {

			@Override
			public void notifyStart(final SimulatedTime st, final GpioProvider provider) {
				ex.execute(new Runnable() {

					

					@Override
					public void run() {
						try {
							logger.debug("Ready to start");
							setReadyForMain();

						} catch (IllegalArgumentException |  SecurityException e) {
							logger.info("main is unable to start");
							if (logger.isTraceEnabled())
								logger.trace("main is unable to start", e);							
							SimulatedTime.INST.stop();
							System.exit(0);
						} 
					}
				});
			}

			@Override
			public void notifyStop(GpioProvider gpioProvider) {
				logger.info("Stop Called");
//				GpioFactory.getExecutorServiceFactory().shutdown();
				SimulatedTime.INST.stop(); // this will cause next sleep to
											// be interrupted!
			}
		});
	}

	public void start(HandlerRunner runnable) {
		runnable.setHandle(this);
		ex.execute(runnable);		
	}

	public CSHandler getCsHandler() {		
		return fmiHandler;
	}

}
