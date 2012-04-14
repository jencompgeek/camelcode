package modules;

import java.net.UnknownHostException;

import models.Model;
import play.Application;
import play.Logger;

import com.google.code.morphia.Datastore;
import com.google.code.morphia.Morphia;
import com.google.code.morphia.mapping.DefaultCreator;
import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.google.inject.Provides;
import com.mongodb.DBObject;
import com.mongodb.Mongo;

/**
 * @author Mathias Bogaert
 * @author Jennifer Hickey
 */
public class MorphiaModule extends AbstractModule {
    @Override
    protected void configure() {
        requireBinding(Application.class);
        requestStaticInjection(Model.class);
    }

    @Provides
    Datastore create(final Application application, final Injector injector) {
        Morphia morphia = new Morphia();
        morphia.getMapper().getOptions().objectFactory = new DefaultCreator() {
            @Override
            protected ClassLoader getClassLoaderForClass(String clazz, DBObject object) {
                return application.classloader();
            }
        };
        morphia.mapPackage("models");

        try {
        	final Mongo mongo = new Mongo(application.configuration().getString("mongodb.host"),application.configuration().getInt("mongodb.port"));
        	String user = application.configuration().getString("mongodb.username");
        	String password = application.configuration().getString("mongodb.password");
        	Datastore datastore;
        	if(user == null || password == null) {
        		datastore = morphia.createDatastore(mongo, application.configuration().getString("mongodb.db"));
        	}else {
        	    datastore = morphia.createDatastore(mongo, application.configuration().getString("mongodb.db"),user,password.toCharArray());
        	}
            datastore.ensureIndexes();

            Logger.info("Connected to MongoDB [" + mongo.debugString() + "] database [" + datastore.getDB().getName() + "]");

            return datastore;
        } catch (UnknownHostException e) {
            addError(e);
            return null;
        }
    }
}
