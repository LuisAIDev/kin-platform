"use client";

import { useEffect, useState } from "react";
import AppointmentForm from "@/components/telemedicine/AppointmentForm";
import AppointmentList from "@/components/telemedicine/AppointmentList";
import { telemedicineService } from "@/services/telemedicine";
import type { Appointment, AppointmentStatus } from "@/services/telemedicine";

export default function AppointmentsPage({
  asPhysician,
  physicianId,
}: {
  asPhysician: boolean;
  physicianId?: string;
}) {
  const [appointments, setAppointments] = useState<Appointment[]>([]);
  const [error, setError] = useState("");

  const load = async () => {
    try {
      const data = await telemedicineService.appointments();
      setAppointments(data);
    } catch (err) {
      setError((err as Error).message);
    }
  };

  useEffect(() => {
    let cancelled = false;
    telemedicineService
      .appointments()
      .then((data) => {
        if (!cancelled) setAppointments(data);
      })
      .catch((err) => {
        if (!cancelled) setError((err as Error).message);
      });
    return () => {
      cancelled = true;
    };
  }, []);

  const handleStatusChange = async (id: string, status: AppointmentStatus) => {
    try {
      await telemedicineService.updateAppointmentStatus(id, status);
      await load();
    } catch (err) {
      setError((err as Error).message);
    }
  };

  return (
    <main className="flex-1 flex items-start justify-center px-6 pt-10 pb-12">
      <div className="w-full max-w-5xl flex flex-col gap-6">
        <div>
          <h1 className="text-2xl font-bold">Citas</h1>
          <p className="text-sm text-neutral-500 mt-1">
            {asPhysician ? "Gestiona las citas de tus pacientes." : "Solicita y consulta tus citas."}
          </p>
        </div>

        {error && (
          <p className="text-sm text-red-600 bg-red-50 px-4 py-2.5 rounded-lg">{error}</p>
        )}

        {!asPhysician && (
          <AppointmentForm
            physicianId={physicianId}
            onCreated={load}
          />
        )}

        <section className="flex flex-col gap-3">
          <h2 className="text-lg font-semibold">Mis citas</h2>
          <AppointmentList
            appointments={appointments}
            asPhysician={asPhysician}
            onStatusChange={asPhysician ? handleStatusChange : undefined}
          />
        </section>
      </div>
    </main>
  );
}
