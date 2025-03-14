package com.example.util;

import jakarta.servlet.FilterRegistration;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import org.springframework.web.WebApplicationInitializer;
import org.springframework.web.context.ContextLoaderListener;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.filter.DelegatingFilterProxy;

public class WeatherInitializer implements WebApplicationInitializer {

    @Override
    public void onStartup(ServletContext servletContext) throws ServletException {

        AnnotationConfigWebApplicationContext context = new AnnotationConfigWebApplicationContext();
        context.register(com.example.config.SpringConfig.class);

        servletContext.addListener(new ContextLoaderListener(context));

        FilterRegistration.Dynamic userFilter = servletContext.addFilter("userFilter",
                new DelegatingFilterProxy("userFilter"));
        userFilter.addMappingForUrlPatterns(null, false, "/*");
    }
}
