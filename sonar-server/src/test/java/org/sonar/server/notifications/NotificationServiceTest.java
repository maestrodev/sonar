/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2011 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.server.notifications;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

import org.junit.Before;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.sonar.api.notifications.Notification;
import org.sonar.api.notifications.NotificationChannel;
import org.sonar.api.notifications.NotificationDispatcher;
import org.sonar.core.notifications.DefaultNotificationManager;
import org.sonar.jpa.entity.NotificationQueueElement;

public class NotificationServiceTest {

  private static String USER_SIMON = "simon";
  private static String USER_EVGENY = "evgeny";

  private NotificationChannel emailChannel;
  private NotificationChannel gtalkChannel;

  private NotificationDispatcher commentOnReviewAssignedToMe;
  private String assignee;
  private NotificationDispatcher commentOnReviewCreatedByMe;
  private String creator;

  private DefaultNotificationManager manager;
  private NotificationService service;

  @Before
  public void setUp() {
    emailChannel = mock(NotificationChannel.class);
    when(emailChannel.getKey()).thenReturn("email");

    gtalkChannel = mock(NotificationChannel.class);
    when(gtalkChannel.getKey()).thenReturn("gtalk");

    commentOnReviewAssignedToMe = mock(NotificationDispatcher.class);
    when(commentOnReviewAssignedToMe.getKey()).thenReturn("comment on review assigned to me");
    doAnswer(new Answer<Object>() {
      public Object answer(InvocationOnMock invocation) throws Throwable {
        ((NotificationDispatcher.Context) invocation.getArguments()[1]).addUser(assignee);
        return null;
      }
    }).when(commentOnReviewAssignedToMe).dispatch(any(Notification.class), any(NotificationDispatcher.Context.class));

    commentOnReviewCreatedByMe = mock(NotificationDispatcher.class);
    when(commentOnReviewCreatedByMe.getKey()).thenReturn("comment on review created by me");
    doAnswer(new Answer<Object>() {
      public Object answer(InvocationOnMock invocation) throws Throwable {
        ((NotificationDispatcher.Context) invocation.getArguments()[1]).addUser(creator);
        return null;
      }
    }).when(commentOnReviewCreatedByMe).dispatch(any(Notification.class), any(NotificationDispatcher.Context.class));

    NotificationDispatcher[] dispatchers = new NotificationDispatcher[] { commentOnReviewAssignedToMe, commentOnReviewCreatedByMe };
    NotificationChannel[] channels = new NotificationChannel[] { emailChannel, gtalkChannel };
    manager = mock(DefaultNotificationManager.class);
    service = spy(new NotificationService(manager, dispatchers, channels));
    doReturn(false).when(service).isEnabled(any(String.class), any(NotificationChannel.class), any(NotificationDispatcher.class));
  }

  @Test
  public void shouldPeriodicallyProcessQueue() throws Exception {
    NotificationQueueElement queueElement = mock(NotificationQueueElement.class);
    Notification notification = mock(Notification.class);
    when(queueElement.getNotification()).thenReturn(notification);
    when(manager.getFromQueue()).thenReturn(queueElement).thenReturn(null);
    doNothing().when(service).deliver(any(Notification.class));

    service.setPeriod(10);
    service.start();
    Thread.sleep(50);
    service.stop();

    verify(service).deliver(notification);
  }

  /**
   * Given:
   * Simon wants to receive notifications by email on comments for reviews assigned to him or created by him.
   *
   * When:
   * Freddy adds comment to review created by Simon and assigned to Simon.
   * 
   * Then:
   * Only one notification should be delivered to Simon by Email.
   */
  @Test
  public void scenario1() {
    doReturn(true).when(service).isEnabled(USER_SIMON, emailChannel, commentOnReviewAssignedToMe);
    doReturn(true).when(service).isEnabled(USER_SIMON, emailChannel, commentOnReviewCreatedByMe);

    Notification notification = mock(Notification.class);
    creator = USER_SIMON;
    assignee = USER_SIMON;

    service.deliver(notification);

    verify(emailChannel).deliver(notification, USER_SIMON);
    verifyNoMoreInteractions(emailChannel);
    verifyNoMoreInteractions(gtalkChannel);
  }

  /**
   * Given:
   * Evgeny wants to receive notification by GTalk on comments for reviews created by him.
   * Simon wants to receive notification by Email on comments for reviews assigned to him.
   * 
   * When:
   * Freddy adds comment to review created by Evgeny and assigned to Simon.
   * 
   * Then:
   * Two notifications should be delivered - one to Simon by Email and another to Evgeny by GTalk.
   */
  @Test
  public void scenario2() {
    doReturn(true).when(service).isEnabled(USER_EVGENY, gtalkChannel, commentOnReviewCreatedByMe);
    doReturn(true).when(service).isEnabled(USER_SIMON, emailChannel, commentOnReviewAssignedToMe);

    Notification notification = mock(Notification.class);
    creator = USER_EVGENY;
    assignee = USER_SIMON;

    service.deliver(notification);

    verify(emailChannel).deliver(notification, USER_SIMON);
    verify(gtalkChannel).deliver(notification, USER_EVGENY);
    verifyNoMoreInteractions(emailChannel);
    verifyNoMoreInteractions(gtalkChannel);
  }

  /**
   * Given:
   * Simon wants to receive notifications by Email and GTLak on comments for reviews assigned to him.
   * 
   * When:
   * Freddy adds comment to review created by Evgeny and assigned to Simon.
   * 
   * Then:
   * Two notifications should be delivered to Simon - one by Email and another by GTalk.
   */
  @Test
  public void scenario3() {
    doReturn(true).when(service).isEnabled(USER_SIMON, emailChannel, commentOnReviewAssignedToMe);
    doReturn(true).when(service).isEnabled(USER_SIMON, gtalkChannel, commentOnReviewAssignedToMe);

    Notification notification = mock(Notification.class);
    creator = USER_EVGENY;
    assignee = USER_SIMON;

    service.deliver(notification);

    verify(emailChannel).deliver(notification, USER_SIMON);
    verify(gtalkChannel).deliver(notification, USER_SIMON);
    verifyNoMoreInteractions(emailChannel);
    verifyNoMoreInteractions(gtalkChannel);
  }

  /**
   * Given:
   * Nobody wants to receive notifications.
   * 
   * When:
   * Freddy adds comment to review created by Evgeny and assigned to Simon.
   * 
   * Then:
   * No notifications.
   */
  @Test
  public void scenario4() {
    Notification notification = mock(Notification.class);
    creator = USER_EVGENY;
    assignee = USER_SIMON;

    service.deliver(notification);

    verifyNoMoreInteractions(emailChannel);
    verifyNoMoreInteractions(gtalkChannel);
  }

}