import { useState } from "react";
import { STATUS_ORDER } from "../constants.js";
import Column from "./Column.jsx";

export default function Board({ tickets, onOpen, onMoveTicket }) {
  const [dragOverStatus, setDragOverStatus] = useState(null);

  function handleDropTicket(id, newStatus) {
    const ticket = tickets.find((t) => t.id === id);
    if (!ticket) return;
    onMoveTicket(ticket, newStatus);
  }

  return (
    <div className="board">
      {STATUS_ORDER.map((status) => (
        <Column
          key={status}
          status={status}
          tickets={tickets.filter((t) => t.status === status)}
          onOpen={onOpen}
          onDropTicket={handleDropTicket}
          dragOverStatus={dragOverStatus}
          setDragOverStatus={setDragOverStatus}
        />
      ))}
    </div>
  );
}
