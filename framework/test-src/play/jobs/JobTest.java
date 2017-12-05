package play.jobs;

import org.junit.Test;
import play.Play;
import play.libs.F;
import play.mvc.Router;
import play.plugins.PluginCollection;

import java.util.concurrent.Callable;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Mockito.*;

@SuppressWarnings("AssignmentToStaticFieldFromInstanceMethod")
public class JobTest {
  @Test
  public void callScheduledJob() {
    mockPlay();
    ScheduledThreadPoolExecutor executor = mock(ScheduledThreadPoolExecutor.class);
    JobsPlugin.executor = executor;

    Job<Void> job = new TestJob();
    job.executor = executor;

    job.call();

    assertThat(job.runOnce).isFalse();
    verify(executor).schedule(eq((Callable<?>) job), anyLong(), eq(TimeUnit.MILLISECONDS));
  }

  @Test
  public void callJob() {
    mockPlay();
    ScheduledThreadPoolExecutor executor = mock(ScheduledThreadPoolExecutor.class);
    JobsPlugin.executor = executor;

    Job<Void> job = new TestJob();

    job.call();

    assertThat(job.runOnce).isFalse();
    verify(executor, never()).schedule(any(Callable.class), anyLong(), any());
  }

  @Test
  public void callScheduledJobOnce() {
    mockPlay();
    ScheduledThreadPoolExecutor executor = mock(ScheduledThreadPoolExecutor.class);
    JobsPlugin.executor = executor;

    Job<Void> job = new TestJob();
    job.executor = executor;
    job.runOnce = true;

    job.call();

    assertThat(job.runOnce).isFalse();
    verify(executor, never()).schedule(any(Callable.class), anyLong(), any());
  }

  @SuppressWarnings("rawtypes")
  private void mockPlay() {
    Play.mode = Play.Mode.PROD;
    Play.started = true;
    Router.lastLoading = 1;
    Play.pluginCollection = mock(PluginCollection.class);

    F.Option filters = mock(F.Option.class);
    when(filters.isDefined()).thenReturn(false);

    when(Play.pluginCollection.composeFilters()).thenReturn(filters);
    when(Play.pluginCollection.detectClassesChange()).thenReturn(true);
  }

  @On("0 0 10 * * ?")
  private static class TestJob extends Job<Void> {}
}
