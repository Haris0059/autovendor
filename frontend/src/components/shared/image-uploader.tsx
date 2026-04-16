"use client";

import { useCallback, useRef } from "react";
import Image from "next/image";
import {
  DndContext,
  PointerSensor,
  closestCenter,
  useSensor,
  useSensors,
  type DragEndEvent,
} from "@dnd-kit/core";
import {
  SortableContext,
  arrayMove,
  rectSortingStrategy,
  useSortable,
} from "@dnd-kit/sortable";
import { CSS } from "@dnd-kit/utilities";
import { GripVerticalIcon, ImagePlusIcon, StarIcon, XIcon } from "lucide-react";

import { Button } from "@/components/ui/button";
import { cn } from "@/lib/utils";

export interface UploaderImage {
  id: string;
  url: string;
  is_main: boolean;
  file?: File;
}

interface ImageUploaderProps {
  images: UploaderImage[];
  onChange: (images: UploaderImage[]) => void;
  maxImages?: number;
  disabled?: boolean;
}

export function ImageUploader({
  images,
  onChange,
  maxImages = 20,
  disabled,
}: ImageUploaderProps) {
  const fileInput = useRef<HTMLInputElement>(null);

  const sensors = useSensors(
    useSensor(PointerSensor, { activationConstraint: { distance: 4 } })
  );

  const handleFiles = useCallback(
    (files: FileList | null) => {
      if (!files || disabled) return;
      const next: UploaderImage[] = [];
      for (const file of Array.from(files)) {
        if (images.length + next.length >= maxImages) break;
        next.push({
          id: `local-${crypto.randomUUID()}`,
          url: URL.createObjectURL(file),
          is_main: images.length === 0 && next.length === 0,
          file,
        });
      }
      onChange([...images, ...next]);
    },
    [images, maxImages, onChange, disabled]
  );

  const handleDragEnd = (event: DragEndEvent) => {
    const { active, over } = event;
    if (!over || active.id === over.id) return;
    const from = images.findIndex((i) => i.id === active.id);
    const to = images.findIndex((i) => i.id === over.id);
    if (from === -1 || to === -1) return;
    onChange(arrayMove(images, from, to));
  };

  const handleRemove = (id: string) => {
    const next = images.filter((i) => i.id !== id);
    if (!next.find((i) => i.is_main) && next.length > 0) {
      next[0] = { ...next[0], is_main: true };
    }
    onChange(next);
  };

  const handleSetMain = (id: string) => {
    onChange(images.map((i) => ({ ...i, is_main: i.id === id })));
  };

  return (
    <div className="flex flex-col gap-4">
      <DndContext
        sensors={sensors}
        collisionDetection={closestCenter}
        onDragEnd={handleDragEnd}
      >
        <SortableContext
          items={images.map((i) => i.id)}
          strategy={rectSortingStrategy}
        >
          <div className="grid grid-cols-2 gap-3 sm:grid-cols-3 md:grid-cols-4">
            {images.map((image) => (
              <SortableImage
                key={image.id}
                image={image}
                onRemove={() => handleRemove(image.id)}
                onSetMain={() => handleSetMain(image.id)}
              />
            ))}
            {images.length < maxImages ? (
              <button
                type="button"
                onClick={() => fileInput.current?.click()}
                disabled={disabled}
                className={cn(
                  "flex aspect-square flex-col items-center justify-center gap-2 rounded-md border border-dashed border-muted-foreground/40 text-sm text-muted-foreground transition-colors hover:border-primary hover:text-primary",
                  disabled && "cursor-not-allowed opacity-50"
                )}
              >
                <ImagePlusIcon className="size-6" />
                Dodaj sliku
              </button>
            ) : null}
          </div>
        </SortableContext>
      </DndContext>
      <input
        ref={fileInput}
        type="file"
        accept="image/*"
        multiple
        hidden
        onChange={(e) => {
          handleFiles(e.target.files);
          e.target.value = "";
        }}
      />
      <p className="text-xs text-muted-foreground">
        {images.length}/{maxImages} slika. Prevucite za promjenu redoslijeda.
        Glavna slika označena zvjezdicom.
      </p>
    </div>
  );
}

interface SortableImageProps {
  image: UploaderImage;
  onRemove: () => void;
  onSetMain: () => void;
}

function SortableImage({ image, onRemove, onSetMain }: SortableImageProps) {
  const { attributes, listeners, setNodeRef, transform, transition, isDragging } =
    useSortable({ id: image.id });

  const style = {
    transform: CSS.Transform.toString(transform),
    transition,
    zIndex: isDragging ? 10 : undefined,
  };

  return (
    <div
      ref={setNodeRef}
      style={style}
      className={cn(
        "group relative aspect-square overflow-hidden rounded-md border bg-muted",
        isDragging && "ring-2 ring-primary"
      )}
    >
      <Image
        src={image.url}
        alt=""
        fill
        sizes="200px"
        className="object-cover"
        unoptimized
      />
      {image.is_main ? (
        <span className="absolute left-2 top-2 flex items-center gap-1 rounded-sm bg-primary px-1.5 py-0.5 text-xs font-medium text-primary-foreground">
          <StarIcon className="size-3" /> Glavna
        </span>
      ) : null}
      <div className="absolute inset-x-2 bottom-2 flex items-center justify-between gap-2 opacity-0 transition-opacity group-hover:opacity-100">
        {!image.is_main ? (
          <Button
            type="button"
            size="sm"
            variant="secondary"
            onClick={onSetMain}
          >
            <StarIcon />
          </Button>
        ) : (
          <span />
        )}
        <div className="flex gap-1">
          <Button
            type="button"
            size="sm"
            variant="secondary"
            {...attributes}
            {...listeners}
          >
            <GripVerticalIcon />
          </Button>
          <Button
            type="button"
            size="sm"
            variant="destructive"
            onClick={onRemove}
          >
            <XIcon />
          </Button>
        </div>
      </div>
    </div>
  );
}
