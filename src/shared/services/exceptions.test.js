/*
 * Copyright (c) 2002-2017 "Neo Technology,"
 * Network Engine for Objects in Lund AB [http://neotechnology.com]
 *
 * This file is part of Neo4j.
 *
 * Neo4j is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
/* global describe, test, expect, jest */
import { createErrorObject, getErrorMessage, BoltConnectionError } from './exceptions'
import * as messages from './exceptionMessages'

describe('getErrorMessage', () => {
  test('should get static messages', () => {
    // Given
    const obj = { type: 'BoltConnectionError' }

    // When
    const msg = getErrorMessage(obj)

    // Then
    expect(msg).toEqual(messages[obj.type])
  })
  test('should interpolate messages', () => {
    // Given
    const obj = { type: 'Neo4jError', message: 'hello' }

    // When
    const msg = getErrorMessage(obj)

    // Then
    expect(msg).toEqual(obj.message)
  })
})

describe('createErrorObject', () => {
  test('createErrorObject should create a valid Error object', () => {
    // Given
    // When
    const obj = createErrorObject(BoltConnectionError)

    // Then
    expect(obj.message).toEqual(messages['BoltConnectionError'])
  })

  test('createErrorObject should pass along parameters', () => {
    // Given
    const fn = jest.fn()

    // When
    createErrorObject(fn, 1, 2, 3)

    // Then
    expect(fn).toHaveBeenCalledWith(1, 2, 3)
  })
})
