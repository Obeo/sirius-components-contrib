/*******************************************************************************
 * Copyright (c) 2019, 2020 Obeo.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Obeo - initial API and implementation
 *******************************************************************************/
import { initialState, reducer } from '../reducer';
import {
  LOADING__STATE,
  FORM_LOADED__STATE,
  COMPLETE__STATE,
  ERROR__STATE,
  HANDLE_DATA__ACTION,
  HANDLE_CONNECTION_ERROR__ACTION,
  HANDLE_UNLOAD__ACTION,
  HANDLE_ERROR__ACTION,
  HANDLE_COMPLETE__ACTION,
} from '../machine';

const formLoadedState = {
  viewState: FORM_LOADED__STATE,
  form: {
    id: 'form',
    label: 'Existing form',
    pages: [],
  },
  subscribers: [],
  widgetSubscriptions: [],
  message: '',
};

const formLoadedWithErrorState = {
  viewState: FORM_LOADED__STATE,
  form: {
    id: 'form',
    label: 'Existing form',
    pages: [],
  },
  subscribers: [],
  widgetSubscriptions: [],
  message: 'An error has occured while retrieving the content from the server',
};

const errorMessage = {
  type: 'error',
  id: '42',
  payload: 'An error has occured while retrieving the content from the server',
};

const completeMessage = {
  type: 'complete',
};

const formRefreshEventPayloadMessage = {
  type: 'data',
  id: '42',
  payload: {
    data: {
      formEvent: {
        __typename: 'FormRefreshedEventPayload',
        form: {
          id: 'form',
          label: 'New Label',
          pages: [],
        },
      },
    },
  },
};

const subscribersUpdatedEventPayloadMessage = {
  type: 'data',
  id: '51',
  payload: {
    data: {
      formEvent: {
        __typename: 'SubscribersUpdatedEventPayload',
        subscribers: [{ username: 'jdoe' }],
      },
    },
  },
};

const widgetSubscriptionsUpdatedEventPayloadMessage = {
  type: 'data',
  id: '54',
  payload: {
    data: {
      formEvent: {
        __typename: 'WidgetSubscriptionsUpdatedEventPayload',
        widgetSubscriptions: [{ widgetId: 'some widget', subscribers: [{ username: 'jdoe' }] }],
      },
    },
  },
};

describe('PropertiesWebSocketContainer - reducer', () => {
  it('has a proper initial state', () => {
    expect(initialState).toStrictEqual({
      viewState: LOADING__STATE,
      form: undefined,
      subscribers: [],
      widgetSubscriptions: [],
      message: 'Please select an object to display its properties',
    });
  });

  it('navigates to the error state if a connection error has been received', () => {
    const prevState = initialState;
    const message = {
      type: 'connection_error',
    };
    const action = { type: HANDLE_CONNECTION_ERROR__ACTION, message };
    const state = reducer(prevState, action);

    expect(state).toStrictEqual({
      viewState: ERROR__STATE,
      form: undefined,
      subscribers: [],
      widgetSubscriptions: [],
      message: 'An error has occured while retrieving the content from the server',
    });
  });

  it('navigates to the error state if an error has been received', () => {
    const prevState = initialState;
    const message = errorMessage;
    const action = { type: HANDLE_ERROR__ACTION, message };
    const state = reducer(prevState, action);

    expect(state).toStrictEqual({
      viewState: ERROR__STATE,
      form: undefined,
      subscribers: [],
      widgetSubscriptions: [],
      message: message.payload,
    });
  });

  it('navigates to the form loaded state if a proper form has been received', () => {
    const prevState = initialState;
    const message = formRefreshEventPayloadMessage;
    const action = { type: HANDLE_DATA__ACTION, message };
    const state = reducer(prevState, action);

    expect(state).toStrictEqual({
      viewState: FORM_LOADED__STATE,
      form: message.payload.data.formEvent.form,
      subscribers: [],
      widgetSubscriptions: [],
      message: '',
    });
  });

  it('refreshes the form if a new form has been received', () => {
    const prevState = formLoadedState;
    const message = formRefreshEventPayloadMessage;
    const action = { type: HANDLE_DATA__ACTION, message };
    const state = reducer(prevState, action);

    expect(state).toStrictEqual({
      viewState: FORM_LOADED__STATE,
      form: message.payload.data.formEvent.form,
      subscribers: [],
      widgetSubscriptions: [],
      message: '',
    });
  });

  it('refreshes the state if an unload action has been received', () => {
    const prevState = formLoadedState;
    const action = { type: HANDLE_UNLOAD__ACTION };
    const state = reducer(prevState, action);

    expect(state).toStrictEqual({
      viewState: LOADING__STATE,
      form: undefined,
      subscribers: [],
      widgetSubscriptions: [],
      message: 'Please select an object to display its properties',
    });
  });

  it('updates the list of subscribers if it should be updated', () => {
    const prevState = formLoadedState;
    const message = subscribersUpdatedEventPayloadMessage;
    const action = { type: HANDLE_DATA__ACTION, message };
    const state = reducer(prevState, action);

    expect(state).toStrictEqual({
      viewState: FORM_LOADED__STATE,
      form: prevState.form,
      subscribers: message.payload.data.formEvent.subscribers,
      widgetSubscriptions: [],
      message: '',
    });
  });

  it('updates the message if an error has been received while a form was displayed', () => {
    const prevState = formLoadedState;
    const message = errorMessage;
    const action = { type: HANDLE_ERROR__ACTION, message };
    const state = reducer(prevState, action);

    expect(state).toStrictEqual({
      viewState: FORM_LOADED__STATE,
      form: prevState.form,
      subscribers: prevState.subscribers,
      widgetSubscriptions: [],
      message: message.payload,
    });
  });

  it('clears the message if a new form has been received', () => {
    const prevState = formLoadedWithErrorState;
    const message = formRefreshEventPayloadMessage;
    const action = { type: HANDLE_DATA__ACTION, message };
    const state = reducer(prevState, action);

    expect(state).toStrictEqual({
      viewState: FORM_LOADED__STATE,
      form: message.payload.data.formEvent.form,
      subscribers: prevState.subscribers,
      widgetSubscriptions: [],
      message: '',
    });
  });

  it('navigates to the complete state if a complete event has been received', () => {
    const prevState = formLoadedState;
    const message = completeMessage;
    const action = { type: HANDLE_COMPLETE__ACTION, message };
    const state = reducer(prevState, action);

    expect(state).toStrictEqual({
      viewState: COMPLETE__STATE,
      form: undefined,
      subscribers: prevState.subscribers,
      widgetSubscriptions: [],
      message: '',
    });
  });

  it('updates the list of widget subscriptions if it should be updated', () => {
    const prevState = formLoadedState;
    const message = widgetSubscriptionsUpdatedEventPayloadMessage;
    const action = { type: HANDLE_DATA__ACTION, message };
    const state = reducer(prevState, action);

    expect(state).toStrictEqual({
      viewState: FORM_LOADED__STATE,
      form: prevState.form,
      subscribers: prevState.subscribers,
      widgetSubscriptions: message.payload.data.formEvent.widgetSubscriptions,
      message: prevState.message,
    });
  });
});
