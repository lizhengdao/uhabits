/*
 * Copyright (C) 2017 Álinson Santos Xavier <isoron@gmail.com>
 *  
 * This file is part of Loop Habit Tracker.
 *  
 * Loop Habit Tracker is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your
 * option) any later version.
 *  
 * Loop Habit Tracker is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *  
 * You should have received a copy of the GNU General Public License along
 * with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package org.isoron.uhabits.core.commands;

import org.isoron.uhabits.core.*;
import org.isoron.uhabits.core.models.*;
import org.isoron.uhabits.core.utils.*;
import org.junit.*;

import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;

public class EditHabitCommandTest extends BaseUnitTest
{
    private EditHabitCommand command;

    private Habit habit;

    private Habit modified;

    private Timestamp today;

    @Override
    @Before
    public void setUp() throws Exception
    {
        super.setUp();

        habit = fixtures.createShortHabit();
        habit.setName("original");
        habit.setFrequency(Frequency.DAILY);
        habit.recompute();
        habitList.add(habit);

        modified = fixtures.createEmptyHabit();
        modified.copyFrom(habit);
        modified.setName("modified");
        habitList.add(modified);

        today =  DateUtils.getTodayWithOffset();
    }

    @Test
    public void testExecute()
    {
        command = new EditHabitCommand(habitList, habit.getId(), modified);

        double originalScore = habit.getScores().get(today).getValue();
        assertThat(habit.getName(), equalTo("original"));

        command.run();
        assertThat(habit.getName(), equalTo("modified"));
        assertThat(habit.getScores().get(today).getValue(), equalTo(originalScore));
    }
}
