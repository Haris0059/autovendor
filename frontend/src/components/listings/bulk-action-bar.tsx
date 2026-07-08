"use client";

import { toast } from "sonner";
import {
  CheckCircle2Icon,
  EyeIcon,
  EyeOffIcon,
  Loader2Icon,
  RefreshCwIcon,
  Trash2Icon,
  XIcon,
} from "lucide-react";

import { ConfirmDialog } from "@/components/shared/confirm-dialog";
import { Button } from "@/components/ui/button";
import {
  useDeleteListing,
  useListingAction,
  LISTING_ACTIONS_BY_STATUS,
  type ListingAction,
} from "@/hooks/use-listings";
import { toastMessages } from "@/lib/toast-messages";
import { useState } from "react";

interface BulkActionBarProps {
  selectedIds: number[];
  /** Status of the currently shown tab — selection can only contain listings of this status. */
  status: string;
  onClear: () => void;
}

type ToastKey = Extract<keyof typeof toastMessages, string>

const ACTIONS: {
  key: ListingAction;
  label: string;
  icon: React.ComponentType<{ className?: string }>;
  toastKey: ToastKey;
}[] = [
  {
    key: "publish",
    label: "Objavi",
    icon: CheckCircle2Icon,
    toastKey: "published",
  },
  {
    key: "refresh",
    label: "Osvježi",
    icon: RefreshCwIcon,
    toastKey: "refreshed",
  },
  { key: "hide", label: "Sakrij", icon: EyeOffIcon, toastKey: "hidden" },
  { key: "unhide", label: "Otkrij", icon: EyeIcon, toastKey: "unhidden" },
];

export function BulkActionBar({ selectedIds, status, onClear }: BulkActionBarProps) {
  const action = useListingAction();
  const remove = useDeleteListing();
  const [deleteOpen, setDeleteOpen] = useState(false);

  const availableActions = ACTIONS.filter(({ key }) =>
    (LISTING_ACTIONS_BY_STATUS[status] ?? []).includes(key)
  );

  const runAction = async (key: ListingAction, toastKey: ToastKey) => {
    for (const id of selectedIds) {
      await action.mutateAsync({ id, action: key });
    }
    toast.success(toastMessages[toastKey] as string);
    onClear();
  };

  const runDelete = async () => {
    for (const id of selectedIds) {
      await remove.mutateAsync(id);
    }
    toast.success(toastMessages.deleted);
    setDeleteOpen(false);
    onClear();
  };

  const disabled = action.isPending || remove.isPending;

  return (
    <>
      <div className="flex flex-wrap items-center gap-2 rounded-md border bg-muted/50 px-3 py-2">
        <span className="text-sm font-medium">
          {selectedIds.length}{" "}
          {selectedIds.length === 1 ? "odabran" : "odabranih"}
        </span>
        <div className="ml-auto flex flex-wrap items-center gap-2">
          {availableActions.map(({ key, label, icon: Icon, toastKey }) => (
            <Button
              key={key}
              size="sm"
              variant="outline"
              disabled={disabled}
              onClick={() => runAction(key, toastKey)}
            >
              {action.isPending ? (
                <Loader2Icon className="animate-spin" />
              ) : (
                <Icon />
              )}
              {label}
            </Button>
          ))}
          <Button
            size="sm"
            variant="destructive"
            disabled={disabled}
            onClick={() => setDeleteOpen(true)}
          >
            <Trash2Icon />
            Obriši
          </Button>
          <Button
            size="sm"
            variant="ghost"
            disabled={disabled}
            onClick={onClear}
          >
            <XIcon />
          </Button>
        </div>
      </div>
      <ConfirmDialog
        open={deleteOpen}
        onOpenChange={setDeleteOpen}
        title={`Obriši ${selectedIds.length} ${
          selectedIds.length === 1 ? "artikal" : "artikala"
        }`}
        description="Ova akcija je nepovratna."
        confirmLabel="Obriši"
        destructive
        loading={remove.isPending}
        onConfirm={runDelete}
      />
    </>
  );
}
