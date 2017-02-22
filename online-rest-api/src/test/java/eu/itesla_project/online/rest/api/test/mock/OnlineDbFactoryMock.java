package eu.itesla_project.online.rest.api.test.mock;

import eu.itesla_project.modules.online.OnlineDb;
import eu.itesla_project.modules.online.OnlineDbFactory;

public class OnlineDbFactoryMock implements OnlineDbFactory {

    @Override
    public OnlineDb create() {
        return new OnlineDbMock();
    }

}
