/**
 * Copyright (c) 2002-2014 "Neo Technology,"
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
package org.neo4j.kernel.impl.transaction.xaframework;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.transaction.xa.Xid;

import org.hamcrest.Description;
import org.hamcrest.Matcher;

import org.hamcrest.TypeSafeMatcher;
import org.neo4j.kernel.impl.nioneo.store.FileSystemAbstraction;
import org.neo4j.kernel.impl.nioneo.xa.LogDeserializer;
import org.neo4j.kernel.impl.nioneo.xa.XaCommandReaderFactory;
import org.neo4j.kernel.impl.util.Consumer;
import org.neo4j.kernel.impl.util.Cursor;
import org.neo4j.kernel.impl.nioneo.store.StoreChannel;

/**
 * A set of hamcrest matchers for asserting logical logs look in certain ways.
 * Please expand as necessary.
 * 
 * Please note: Matching specific commands is done by matchers found in
 * {@link org.neo4j.kernel.impl.nioneo.xa.CommandMatchers}.
 */
public class LogMatchers
{
    public static List<LogEntry> logEntries( FileSystemAbstraction fileSystem, String logPath ) throws IOException
    {
        StoreChannel fileChannel = fileSystem.open( new File( logPath ), "r" );
        ByteBuffer buffer = ByteBuffer.allocateDirect( 9 + Xid.MAXGTRIDSIZE + Xid.MAXBQUALSIZE * 10 );

        try
        {
            // Always a header
            VersionAwareLogEntryReader.readLogHeader( buffer, fileChannel, true );

            // Read all log entries
            final List<LogEntry> entries = new ArrayList<>();
            LogDeserializer deserializer = new LogDeserializer( XaCommandReaderFactory.DEFAULT );


            Consumer<LogEntry, IOException> consumer = new Consumer<LogEntry, IOException>()
            {
                @Override
                public boolean accept( LogEntry entry ) throws IOException
                {
                    entries.add( entry );
                    return true;
                }
            };

            try( Cursor<LogEntry, IOException> cursor = deserializer.cursor( fileChannel ) )
            {
                while ( cursor.next( consumer ) );
            }

            return entries;
        }
        finally
        {
            fileChannel.close();
        }
    }

    public static List<LogEntry> logEntries( FileSystemAbstraction fileSystem, File file ) throws IOException
    {
        return logEntries( fileSystem, file.getPath() );
    }

    public static Matcher<Iterable<LogEntry>> containsExactly( final Matcher<? extends LogEntry>... matchers )
    {
        return new TypeSafeMatcher<Iterable<LogEntry>>()
        {
            @Override
            public boolean matchesSafely( Iterable<LogEntry> item )
            {
                Iterator<LogEntry> actualEntries = item.iterator();
                for ( Matcher<? extends LogEntry> matcher : matchers )
                {
                    if ( actualEntries.hasNext() )
                    {
                        LogEntry next = actualEntries.next();
                        if ( !matcher.matches( next ) )
                        {
                            // Wrong!
                            return false;
                        }
                    }
                    else
                    {
                        // Too few actual entries!
                        return false;
                    }
                }

                if ( actualEntries.hasNext() )
                {
                    // Too many actual entries!
                    return false;
                }

                // All good in the hood :)
                return true;
            }

            @Override
            public void describeTo( Description description )
            {
                for ( Matcher<? extends LogEntry> matcher : matchers )
                {
                    description.appendDescriptionOf( matcher ).appendText( ",\n" );
                }
            }
        };
    }

    public static Matcher<? extends LogEntry> startEntry( final Integer identifier, final int masterId,
            final int localId )
    {
        return new TypeSafeMatcher<LogEntry.Start>()
        {

            @Override
            public boolean matchesSafely( LogEntry.Start entry )
            {
                return entry != null && entry.getMasterId() == masterId
                        && entry.getLocalId() == localId;
            }

            @Override
            public void describeTo( Description description )
            {
                description.appendText( "Start[" + identifier + ",xid=<Any Xid>,master=" + masterId + ",me=" + localId
                        + ",time=<Any Date>]" );
            }
        };
    }

    public static Matcher<? extends LogEntry> onePhaseCommitEntry( final int identifier, final int txId )
    {
        return new TypeSafeMatcher<LogEntry.OnePhaseCommit>()
        {

            @Override
            public boolean matchesSafely( LogEntry.OnePhaseCommit onePC )
            {
                return onePC != null && onePC.getTxId() == txId;
            }

            @Override
            public void describeTo( Description description )
            {
                description.appendText( String.format( "1PC[%d, txId=%d, <Any Date>],", identifier, txId ) );

            }
        };
    }
}
