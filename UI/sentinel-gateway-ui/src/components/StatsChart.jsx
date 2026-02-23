import React from 'react';
import { AreaChart, Area, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer } from 'recharts';

const data = [
    { time: '10:00', requests: 400, errors: 24, latency: 45 },
    { time: '10:30', requests: 300, errors: 13, latency: 38 },
    { time: '11:00', requests: 600, errors: 98, latency: 52 },
    { time: '11:30', requests: 800, errors: 32, latency: 41 },
    { time: '12:00', requests: 500, errors: 12, latency: 36 },
    { time: '12:30', requests: 900, errors: 45, latency: 48 },
    { time: '13:00', requests: 1100, errors: 67, latency: 55 },
];

const StatsChart = ({ type = 'requests' }) => {
    const color = type === 'requests' ? '#a855f7' : type === 'errors' ? '#ef4444' : '#3b82f6';

    return (
        <div className="w-full h-full">
            <ResponsiveContainer width="100%" height="100%">
                <AreaChart data={data} margin={{ top: 10, right: 10, left: 0, bottom: 0 }}>
                    <defs>
                        <linearGradient id="colorValue" x1="0" y1="0" x2="0" y2="1">
                            <stop offset="5%" stopColor={color} stopOpacity={0.3} />
                            <stop offset="95%" stopColor={color} stopOpacity={0} />
                        </linearGradient>
                    </defs>
                    <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="rgba(255,255,255,0.05)" />
                    <XAxis
                        dataKey="time"
                        axisLine={false}
                        tickLine={false}
                        tick={{ fill: '#64748b', fontSize: 10 }}
                        dy={10}
                    />
                    <YAxis
                        axisLine={false}
                        tickLine={false}
                        tick={{ fill: '#64748b', fontSize: 10 }}
                    />
                    <Tooltip
                        contentStyle={{ backgroundColor: '#0d0d0f', border: '1px solid rgba(255,255,255,0.1)', borderRadius: '12px' }}
                        itemStyle={{ fontSize: '12px' }}
                    />
                    <Area
                        type="monotone"
                        dataKey={type}
                        stroke={color}
                        fillOpacity={1}
                        fill="url(#colorValue)"
                        strokeWidth={3}
                    />
                </AreaChart>
            </ResponsiveContainer>
        </div>
    );
};

export default StatsChart;
