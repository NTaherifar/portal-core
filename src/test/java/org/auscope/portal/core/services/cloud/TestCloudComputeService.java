package org.auscope.portal.core.services.cloud;

import java.util.ArrayList;
import java.util.Arrays;

import junit.framework.Assert;

import org.auscope.portal.core.cloud.CloudJob;
import org.auscope.portal.core.services.PortalServiceException;
import org.auscope.portal.core.test.PortalTestClass;
import org.jmock.Expectations;
import org.junit.Before;
import org.junit.Test;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.Reservation;
import com.amazonaws.services.ec2.model.RunInstancesRequest;
import com.amazonaws.services.ec2.model.RunInstancesResult;
import com.amazonaws.services.ec2.model.TerminateInstancesRequest;
import com.amazonaws.services.ec2.model.TerminateInstancesResult;

public class TestCloudComputeService extends PortalTestClass {
    /**
     * For testing the protected CloudComputeService methods
     */
    private class TestableCloudComputeService extends CloudComputeService {
        AmazonEC2 mockAmazonEC2;

        public TestableCloudComputeService(AmazonEC2 mockAmazonEC2,String endpoint, AWSCredentials credentials) {
            super(endpoint, credentials);
            this.mockAmazonEC2 = mockAmazonEC2;
        }

        @Override
        protected AmazonEC2 getAmazonEC2Instance() {
            return mockAmazonEC2;
        }
    }

    private final AmazonEC2 mockAmazonEC2 = context.mock(AmazonEC2.class);
    private final String ec2Endpoint = "http://example.org/ec2";
    private final AWSCredentials mockCredentials = context.mock(AWSCredentials.class);
    private CloudComputeService service;
    private CloudJob job;

    @Before
    public void initJobObject() {
        job = new CloudJob(13);
        service = new TestableCloudComputeService(mockAmazonEC2, ec2Endpoint, mockCredentials);
    }

    /**
     * Tests that job execution correctly calls and parses a response from AmazonEC2
     */
    @Test
    public void testExecuteJob() throws Exception {
        final String userDataString = "user-data-string";
        final String expectedInstanceId = "instance-id";

        final RunInstancesResult runInstanceResult = new RunInstancesResult();
        final Reservation reservation = new Reservation();
        final Instance instance = new Instance();

        instance.setInstanceId(expectedInstanceId);
        reservation.setInstances(Arrays.asList(instance));
        runInstanceResult.setReservation(reservation);

        context.checking(new Expectations() {{
            oneOf(mockAmazonEC2).runInstances(with(any(RunInstancesRequest.class)));will(returnValue(runInstanceResult));
        }});

        String actualInstanceId = service.executeJob(job, userDataString);

        Assert.assertEquals(expectedInstanceId, actualInstanceId);
    }

    /**
     * Tests that job execution correctly calls and parses a response from AmazonEC2
     * when EC2 reports failure by returning 0 running instances.
     */
    @Test(expected=Exception.class)
    public void testExecuteJobFailure() throws Exception {
        final String userDataString = "user-data-string";

        final RunInstancesResult runInstanceResult = new RunInstancesResult();
        final Reservation reservation = new Reservation();

        reservation.setInstances(new ArrayList<Instance>());
        runInstanceResult.setReservation(reservation);

        context.checking(new Expectations() {{
            oneOf(mockAmazonEC2).runInstances(with(any(RunInstancesRequest.class)));will(returnValue(runInstanceResult));
        }});

        service.executeJob(job, userDataString);
    }

    /**
     * Tests that job execution correctly calls and parses a response from AmazonEC2
     * when EC2 reports failure by throwing an exception
     */
    @Test(expected=PortalServiceException.class)
    public void testExecuteJobException() throws Exception {
        final String userDataString = "user-data-string";

        final RunInstancesResult runInstanceResult = new RunInstancesResult();
        final Reservation reservation = new Reservation();

        reservation.setInstances(new ArrayList<Instance>());
        runInstanceResult.setReservation(reservation);

        context.checking(new Expectations() {{
            oneOf(mockAmazonEC2).runInstances(with(any(RunInstancesRequest.class)));will(throwException(new PortalServiceException("")));
        }});

        service.executeJob(job, userDataString);
    }

    /**
     * Tests that job terminate correctly calls AmazonEC2
     */
    @Test
    public void testTerminateJob() {
        final TerminateInstancesResult terminateInstanceResult = new TerminateInstancesResult();

        context.checking(new Expectations() {{
            oneOf(mockAmazonEC2).terminateInstances(with(any(TerminateInstancesRequest.class)));will(returnValue(terminateInstanceResult));
        }});

        service.terminateJob(job);
    }
}