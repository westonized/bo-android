package org.blitzortung.android.alert.object;

import org.blitzortung.android.alert.AlertParameters;
import org.blitzortung.android.alert.factory.AlertObjectFactory;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
public class AlertSectorTest {

    @Mock
    private AlertObjectFactory alertObjectFactory;

    @Mock
    private AlertParameters alertParameters;

    private final String sectorLabel = "foo";

    @Mock
    private AlertSectorRange alertSectorRange1;

    @Mock
    private AlertSectorRange alertSectorRange2;

    private float minimumBearing = 10f;

    private float maximumBearing = 20f;

    private AlertSector alertSector;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        when(alertParameters.getSectorLabels()).thenReturn(new String[]{"eins", "zwei"});
        when(alertParameters.getRangeSteps()).thenReturn(new float[]{10f, 20f});
        when(alertObjectFactory.createAlarmSectorRange(0.0f, 10.0f)).thenReturn(alertSectorRange1);
        when(alertObjectFactory.createAlarmSectorRange(10.0f, 20.0f)).thenReturn(alertSectorRange2);

        alertSector = new AlertSector(alertObjectFactory, alertParameters, sectorLabel, minimumBearing, maximumBearing);
    }

    @Test
    public void testClearResults() {
        alertSector.updateClosestStrikeDistance(10.0f);
        
        alertSector.clearResults();

        //assertThat(alertSector.getClosestStrikeDistance()).isEqualTo(Float.POSITIVE_INFINITY);
        verify(alertSectorRange1, times(1)).clearResults();
        verify(alertSectorRange2, times(1)).clearResults();
    }

    @Test
    public void testGetRanges() {
        final List<AlertSectorRange> ranges = alertSector.getRanges();

        assertThat(ranges).isNotNull();
        assertThat(ranges).contains(alertSectorRange1, alertSectorRange2);
    }

    @Test
    public void testGetMinimumSectorBearing() {
        assertThat(alertSector.getMinimumSectorBearing()).isEqualTo(minimumBearing);
    }

    @Test
    public void testGetMaximumSectorBearing() {
        assertThat(alertSector.getMaximumSectorBearing()).isEqualTo(maximumBearing);
    }
    
    @Test
    public void testGetLabel() {
        assertThat(alertSector.getLabel()).isEqualTo(sectorLabel);
    }
    
    @Test
    public void testGetClosestStrikeDistanceAndUpdateClosestStrikeDistance()
    {
        assertThat(alertSector.getClosestStrikeDistance()).isEqualTo(Float.POSITIVE_INFINITY);
        
        alertSector.updateClosestStrikeDistance(25.0f);

        assertThat(alertSector.getClosestStrikeDistance()).isEqualTo(25f);

        alertSector.updateClosestStrikeDistance(10.0f);

        assertThat(alertSector.getClosestStrikeDistance()).isEqualTo(10f);

        alertSector.updateClosestStrikeDistance(25.0f);

        assertThat(alertSector.getClosestStrikeDistance()).isEqualTo(10f);
    }

}
