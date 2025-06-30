# WebSocket Event Deduplication Solution

## Problem

Users were experiencing duplicate events in the frontend:

1. API response when they performed an action (e.g., creating a task)
2. WebSocket real-time update for the same action

This caused duplicate entries in the UI, particularly visible in project detail and task detail sections.

## Root Cause

The notification service was publishing events to two different topics:

- Direct domain events to `websocket-dispatch` topic for real-time updates
- Processed notifications to `notifications-to-send` topic for user notifications

The WebSocket service consumed from both topics, causing the same logical event to be processed twice on the frontend.

## Solution: Originator Filtering

Instead of removing the dual-topic architecture (which would break real-time updates), we implemented **originator filtering** to prevent users from receiving WebSocket events for actions they themselves initiated.

### Key Changes

1. **Enhanced SubscriptionRegistry** (`websocket-service`)

   - Added overloaded `sendToTopic(topic, envelope, excludeUserId)` method
   - Automatically skips sending to `user:<excludeUserId>` topics
   - Maintains backward compatibility with existing `sendToTopic(topic, envelope)` calls

2. **Enhanced WebSocketEventDispatcher** (`websocket-service`)
   - Added `extractOriginatorUserId()` method to extract user ID from domain events
   - Supports TaskCreated, TaskUpdated, TaskStatusChanged, TaskAssigned, TaskPriorityChanged events
   - Supports CommentAdded, CommentEdited, CommentDeleted events
   - Supports ProjectCreated events
   - Includes fallback handling for Map-based deserialization
   - Automatically excludes originator from WebSocket broadcasts

### How It Works

1. **User creates a task via API**

   ```
   POST /api/tasks → Task Service → API Response (user sees immediate feedback)
   ```

2. **Task service publishes domain event**

   ```
   TaskCreatedEventPayload → task-events topic
   ```

3. **Notification service processes the event**

   ```
   - Creates notification for assignee
   - Publishes domain event to websocket-dispatch (for real-time updates)
   - Publishes NotificationToSendEventPayload to notifications-to-send (for notifications)
   ```

4. **WebSocket service processes events with filtering**

   ```
   - Extracts originator ID from TaskCreatedEventPayload (createdBy field)
   - Sends real-time update to project subscribers EXCEPT the originator
   - Sends notification to assignee EXCEPT if assignee is the originator
   ```

5. **Result**
   - ✅ User sees immediate API response (no duplicate)
   - ✅ Other project members see real-time task creation
   - ✅ Assignee gets notification (unless they created the task themselves)

### Benefits

- **No architectural changes**: Preserves existing real-time update patterns
- **Backward compatible**: Existing code continues to work
- **Granular control**: Only filters out events for the specific user who triggered them
- **Maintains real-time collaboration**: Other users still get immediate updates
- **Simple implementation**: Single method change with clear logic
- **Smart comment handling**: Comments are not deduplicated to ensure immediate feedback

### Comment Events: Special Handling

Comment events (add/edit/delete) are intentionally excluded from deduplication for several important reasons:

1. **Immediate Feedback**: Users expect to see their comments appear instantly after posting
2. **Multi-tab Support**: If a user has multiple tabs open, their comments should appear in all tabs
3. **Collaborative Editing**: Real-time comment updates are crucial for team collaboration
4. **Visual Confirmation**: Users need instant visual feedback that their comment was successfully posted

This means for comments:

- ✅ User sees API response (comment in response payload)
- ✅ User also receives WebSocket update (real-time confirmation)
- ✅ Other users receive real-time updates
- ✅ All tabs/sessions see the comment immediately

The potential "duplicate" for comments is actually desired behavior that enhances user experience.

### Supported Event Types

| Event Type          | Originator Field       | Deduplication Applied | Reason             |
| ------------------- | ---------------------- | --------------------- | ------------------ |
| TaskCreated         | `taskDto.createdBy`    | ✅ Yes                | Prevent duplicates |
| TaskUpdated         | `taskDto.updatedBy`    | ✅ Yes                | Prevent duplicates |
| TaskStatusChanged   | `taskDto.updatedBy`    | ✅ Yes                | Prevent duplicates |
| TaskAssigned        | `taskDto.updatedBy`    | ✅ Yes                | Prevent duplicates |
| TaskPriorityChanged | `dto.updatedBy`        | ✅ Yes                | Prevent duplicates |
| CommentAdded        | N/A                    | ❌ No                 | Real-time feedback |
| CommentEdited       | N/A                    | ❌ No                 | Real-time feedback |
| CommentDeleted      | N/A                    | ❌ No                 | Real-time feedback |
| ProjectCreated      | `projectDto.createdBy` | ✅ Yes                | Prevent duplicates |

**Note**: Comment events are intentionally NOT deduplicated because users expect immediate real-time feedback when posting, editing, or deleting comments. This supports collaborative editing and multi-tab scenarios.

### Alternative Approaches Considered

1. **Remove dual-topic architecture**: Would break real-time updates ❌
2. **Client-side deduplication**: More complex, requires maintaining seen ID sets ⚠️
3. **Originator filtering (chosen)**: Clean, server-side, maintains all functionality ✅

### Testing

To verify the fix:

1. Create a task while monitoring WebSocket events
2. Check that you receive API response but no WebSocket echo
3. Verify other users still receive real-time updates
4. Confirm notifications work for assignees who didn't create the task
