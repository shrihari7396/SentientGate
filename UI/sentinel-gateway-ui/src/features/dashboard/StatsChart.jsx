import React, { useEffect, useState } from 'react';
import { AreaChart, Area, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer } from 'recharts';
import { logApi } from '../../shared/api/client';

const StatsChart = ({ windowMinutes = 30, refreshInterval = 5000 }) => {
    const [chartData, setChartData] = useState([]);

    const fetchVelocity = () => {
        const end = new Date();
        const start = new Date(Date.now() - windowMinutes * 60000);

        logApi.getTrafficVelocity(start.toISOString(), end.toISOString())
            .then(res => {
                const formatted = (res || []).map(bucket => ({
                    time: new Date(bucket.minute).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
                    requests: bucket.requestCount,
                    errors: bucket.errorCount,
                    rateLimited: bucket.rateLimitedCount
                }));
                setChartData(formatted);
            })
            .catch(err => {
                console.error("Chart data fetch failed", err);
            });
    };

    useEffect(() => {
        fetchVelocity();
        if (!refreshInterval) return undefined;
        const interval = setInterval(fetchVelocity, refreshInterval);
        return () => clearInterval(interval);
    }, [windowMinutes, refreshInterval]);

    return (
        <div className="w-full h-[400px] min-h-[400px]">
            <ResponsiveContainer width="100%" height="100%">
                <AreaChart data={chartData} margin={{ top: 10, right: 10, left: 0, bottom: 0 }}>
                    <defs>
                        <linearGradient id="colorRequests" x1="0" y1="0" x2="0" y2="1">
                            <stop offset="5%" stopColor="#a855f7" stopOpacity={0.3} />
                            <stop offset="95%" stopColor="#a855f7" stopOpacity={0} />
                        </linearGradient>
                        <linearGradient id="colorErrors" x1="0" y1="0" x2="0" y2="1">
                            <stop offset="5%" stopColor="#ef4444" stopOpacity={0.3} />
                            <stop offset="95%" stopColor="#ef4444" stopOpacity={0} />
                        </linearGradient>
                    </defs>
                    <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="rgba(255,255,255,0.05)" />
                    <XAxis
                        dataKey="time"
                        axisLine={false}
                        tickLine={false}
                        tick={{ fill: '#64748b', fontSize: 10 }}
                        dy={10}
                        minTickGap={30}
                    />
                    <YAxis
                        axisLine={false}
                        tickLine={false}
                        tick={{ fill: '#64748b', fontSize: 10 }}
                    />
                    <Tooltip
                        contentStyle={{
                            backgroundColor: 'rgba(13, 13, 15, 0.95)',
                            border: '1px solid rgba(255,255,255,0.1)',
                            borderRadius: '16px',
                            backdropFilter: 'blur(10px)',
                            boxShadow: '0 20px 40px rgba(0,0,0,0.4)'
                        }}
                        itemStyle={{ fontSize: '11px', fontWeight: 'bold' }}
                    />
                    <Area
                        type="monotone"
                        dataKey="requests"
                        stroke="#a855f7"
                        fillOpacity={1}
                        fill="url(#colorRequests)"
                        strokeWidth={3}
                        animationDuration={1500}
                    />
                    <Area
                        type="monotone"
                        dataKey="errors"
                        stroke="#ef4444"
                        fillOpacity={1}
                        fill="url(#colorErrors)"
                        strokeWidth={3}
                        animationDuration={2000}
                    />
                </AreaChart>
            </ResponsiveContainer>
        </div>
    );
};

export default StatsChart;
